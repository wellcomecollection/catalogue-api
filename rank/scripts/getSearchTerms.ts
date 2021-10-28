import { code, info, p, pretty, success } from './utils'

import { SearchResponse } from '../types/elasticsearch'
import fs from 'fs'
import { getReportingClient } from '../services/elasticsearch'
import { namespaces } from '../types/searchTemplate'
import prompts from 'prompts'

global.fetch = require('node-fetch')

async function go() {
  // get some real queries from the reporting index
  //   nb get unique ones using a terms aggregation
  //   https://stackoverflow.com/questions/25465215/elasticsearch-return-unique-values
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
    const searchResp = await getReportingClient()
      .search({
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
      .then((res) => res.body)
    success(`Fetched recent search terms for ${namespace}`)

    const terms = {
      terms: searchResp.hits.hits
        .map((hit) => hit._source.page.query.query)
        .filter((item, i, ar) => ar.indexOf(item) === i),
    }
    fs.writeFileSync(p([`../data/terms/${namespace}.json`]), pretty(terms))
    success(`Wrote terms to ./data/terms/${namespace}.json\n`)
  })
}

go()
