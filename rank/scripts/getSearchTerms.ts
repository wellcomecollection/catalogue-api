import { p, pretty, success } from './utils'

import fs from 'fs'
import { getReportingClient } from '../services/elasticsearch'
import { namespaces } from '../types/searchTemplate'
import prompts from 'prompts'

global.fetch = require('node-fetch')

async function go() {
  const chosenNamespaces: string[] = await prompts({
    type: 'multiselect',
    name: 'value',
    message: 'Which namespace do you want to get search terms for?',
    choices: namespaces.map((namespace) => ({
      title: namespace,
      value: namespace,
    })),
  }).then(({ value }) => value)

  chosenNamespaces.map(async (namespace) => {
    const client = await getReportingClient()
    const searchResp = await client.search<Record<string, any>>({
      index: 'metrics-conversion-prod',
      body: {
        size: 5000,
        _source: ['page.query.query'],
        query: {
          bool: {
            must: [
              {
                exists: {
                  field: 'page.query.query',
                },
              },
              {
                match: {
                  'page.name': namespace,
                },
              },
            ],
          },
        },
      },
    })
    success(`Fetched recent search terms for ${namespace}`)

    const terms = searchResp.hits.hits
      .map((hit) => hit._source.page.query.query)
      .filter((term, i, ar) => ar.indexOf(term) === i) // keep unique

    fs.writeFileSync(p([`../terms/${namespace}.json`]), pretty({ terms }))
    success(`Wrote terms to ./terms/${namespace}.json\n`)
  })
}

go()
