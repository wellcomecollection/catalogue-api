import { p, pretty, success } from './utils'

import fetch from 'node-fetch'
import fs from 'fs'
import { getReportingClient } from '../src/services/elasticsearch'
import { namespaces } from '../src/types/searchTemplate'
import prompts from 'prompts'

async function go() {
  const chosenNamespaces: string[] = await prompts({
    type: 'multiselect',
    name: 'value',
    message: 'Which namespace do you want to get search terms for?',
    choices: namespaces.map((namespace) => ({
      title: namespace,
      value: namespace
    }))
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
                  field: 'page.query.query'
                }
              },
              {
                match: {
                  'page.name': namespace
                }
              }
            ]
          }
        }
      }
    })
    success(`Fetched recent search terms for ${namespace}`)

    const terms = searchResp.hits.hits
      .map((hit) => hit._source.page.query.query)
      .filter((term, i, ar) => ar.indexOf(term) === i) // keep unique

    fs.writeFileSync(p([`../data/terms/${namespace}.json`]), pretty({ terms }))
    success(`Wrote terms to ./data/terms/${namespace}.json\n`)
  })
}

go()
