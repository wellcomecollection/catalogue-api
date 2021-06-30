import fs from 'fs'
import prompts from 'prompts'
import { code, info, p, pretty, success } from './utils'
import {
  getRemoteTemplates,
  SearchTemplate,
} from '../services/search-templates'
import { getRankClient } from '../services/elasticsearch'
import { getNamespaceFromIndexName } from '../types/namespace'
import yargs from 'yargs/yargs'
import { hideBin } from 'yargs/helpers'
import searchTemplates from '../pages/api/search-templates'

global.fetch = require('node-fetch')

async function go(args: typeof argv) {
  const { name } = args
  const client = getRankClient()
  const templates = await getRemoteTemplates('prod')
  const indices = templates.map((template) => template.index)

  info(`Getting settings for index config for ${indices}`)
  const settingsReq = client.indices.getSettings({
    index: indices,
  })
  const mappingsReq = client.indices.getMapping({
    index: indices,
  })
  const [{ body: settingsRes }, { body: mappingsRes }] = await Promise.all([
    settingsReq,
    mappingsReq,
  ])

  for (const template of templates) {
    const index = `${template.namespace}-${name}`
    const filename = `${index}.json`
    const mappings = mappingsRes[template.index].mappings
    const analysis = settingsRes[template.index].settings.index.analysis

    const indexConfig = {
      mappings,
      settings: {
        index: {
          analysis,
        },
      },
    }

    info(`Writing index config to ./data/indices/${filename}`)
    fs.writeFileSync(p([`../data/indices/${filename}`]), pretty(indexConfig))

    // We could have a prompt here to create and start the reindex,
    // but more often than not, you're going to want to change the mappings first
    success(
      `New config files created. Edit the mappings in ./data/indices/${filename}, then run \n`
    )
    code(`  yarn createIndex --from ${index} --reindex \n`)
  }
}

const argv = yargs(hideBin(process.argv))
  .options({
    name: { type: 'string', default: 'candidate' },
  })
  .parseSync()

go(argv)
