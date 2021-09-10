import { code, info, p, pretty, success } from './utils'

import fs from 'fs'
import { getRankClient } from '../services/elasticsearch'
import { listIndices } from '../services/search-templates'
import prompts from 'prompts'

global.fetch = require('node-fetch')

async function go() {
  const allIndices = await listIndices()
  const indices: string[] = await prompts({
    type: 'multiselect',
    name: 'value',
    message: 'Which index configs do you want to fetch?',
    choices: allIndices.map((index) => ({ title: index, value: index })),
  }).then(({ value }) => value)

  if (indices.length > 0) {
    const client = getRankClient()
    const settingsReq = client.indices.getSettings({ index: indices })
    const mappingsReq = client.indices.getMapping({ index: indices })
    const [{ body: settingsRes }, { body: mappingsRes }] = await Promise.all([
      settingsReq,
      mappingsReq,
    ])

    for (const index of indices) {
      const mappings = mappingsRes[index].mappings
      const analysis = settingsRes[index].settings.index.analysis

      const indexConfig = {
        mappings,
        settings: {
          index: {
            analysis,
          },
        },
      }
      success(`Fetched config for ${index}`)

      fs.writeFileSync(
        p([`../data/indices/${index}.json`]),
        pretty(indexConfig)
      )
      success(`Wrote config to ./data/indices/${index}.json\n`)
    }
    info(
      `To create a new index with a candidate mapping, edit one of the files in ./data/indices and run\n`
    )
    code(`  yarn createIndex --reindex \n`)
  }
}

go()
