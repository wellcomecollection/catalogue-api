import { code, info, p, pretty, success } from './utils'

import fetch from 'node-fetch'
import fs from 'fs'
import { getRankClient } from '../src/services/elasticsearch'
import { listIndices } from '../src/services/search-templates'
import prompts from 'prompts'

async function go() {
  const client = await getRankClient()
  const remoteIndices = await listIndices(client)
  const indices: string[] = await prompts({
    type: 'multiselect',
    name: 'value',
    message: 'Which index configs do you want to fetch?',
    choices: remoteIndices.map((index) => ({ title: index, value: index }))
  }).then(({ value }) => value)

  if (indices.length > 0) {
    const client = await getRankClient()
    const settingsRes = await client.indices.getSettings({ index: indices })

    const mappingsRes = await client.indices.getMapping({ index: indices })

    for (const index of indices) {
      const mappings = mappingsRes[index].mappings
      const analysis = settingsRes[index].settings.index.analysis

      const indexConfig = {
        mappings,
        settings: {
          index: {
            analysis
          }
        }
      }
      success(`Fetched config for ${index}`)

      fs.writeFileSync(
        p([`../data/mappings/${index}.json`]),
        pretty(indexConfig)
      )
      success(`Wrote config to ./data/mappings/${index}.json\n`)
    }
    info(
      `To create a new index with a candidate mapping, edit one of the files in ./data/mappings and run`
    )
    code(`yarn createIndex`)
  }
}

go()
