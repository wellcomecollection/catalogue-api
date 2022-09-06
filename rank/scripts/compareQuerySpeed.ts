import {
  QueryEnv,
  SearchTemplate,
  namespaces,
  queryEnvs,
} from '../types/searchTemplate'
import { getQueries, listIndices } from '../services/search-templates'
import { histogram, info } from './utils'

import bars from 'bars'
import { getRankClient } from '../services/elasticsearch'
import prompts from 'prompts'
import { MsearchMultiSearchItem } from '@elastic/elasticsearch/lib/api/types'

global.fetch = require('node-fetch')

async function go() {
  const queries = await getQueries()
  const indices = await listIndices()

  const namespace: string = await prompts({
    type: 'select',
    name: 'value',
    message: 'Which namespace do you want to test in?',
    choices: namespaces.map((namespace) => ({
      title: namespace,
      value: namespace,
    })),
  }).then(({ value }) => value)
  const testIndices = indices.filter((index) => index.startsWith(namespace))
  info(
    `Found ${testIndices.length} indices for testing: ${testIndices.toString()}`
  )

  let searchTemplates: SearchTemplate[] = []
  for (const index of testIndices) {
    const queryEnv: QueryEnv = await prompts({
      type: 'select',
      name: 'value',
      message: `Which query should be used with ${index}?`,
      choices: queryEnvs.map((env) => ({
        title: env,
        value: env,
      })),
    }).then(({ value }) => value)
    const query = queries[queryEnv][namespace]
    searchTemplates.push(new SearchTemplate(queryEnv, index, query))
  }

  info('Loading real search terms for querying')
  const { terms } = await import(`../data/terms/${namespace}.json`).then(
    (mod) => mod.default
  )

  // to minimise the effect of network issues, caching etc, between candidates,
  // we use the multisearch API and make concurrent requests to multiple indices.
  info('Running queries\n')
  const multiGetTimes: [number, number][] = await Promise.all(
    terms.map(async (term) => {
      const body = searchTemplates.flatMap(({ index, query }) => [
        { index },
        {
          // replace {{query}} with the actual search term
          query: JSON.parse(
            JSON.stringify(query).replace('{{query}}', encodeURIComponent(term))
          ),
        },
      ])
      const client = await getRankClient()
      const searchResp = await client.msearch({ body })
      return searchResp.responses
        .filter((r): r is MsearchMultiSearchItem => !('error' in r))
        .map((r) => r.took)
    })
  )

  testIndices.map((index, i) => {
    const times = multiGetTimes.map((x) => x[i])
    info(index)
    info(`mean: ${times.reduce((a, b) => a + b, 0) / times.length}`)
    console.log(bars(histogram(times)))
  })
}

go()
