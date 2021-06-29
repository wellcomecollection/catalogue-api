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

global.fetch = require('node-fetch')

async function go(args: typeof argv) {
  const client = getRankClient()
  const { name } = args

  info(`Fetching search templates`)
  const templates = await getRemoteTemplates('prod')
  const localTemplates: SearchTemplate[] = templates.map((template) => ({
    ...template,
    id: `local/${getNamespaceFromIndexName(template.index)}/${name}`,
    env: 'local',
  }))

  info(`Writing local search templates`)
  for (const template of localTemplates) {
    const filename = `${template.namespace}-${name}.json`
    info(`Writing search templates to ./data/search-templates/${filename}`)

    fs.writeFileSync(
      p([`../data/search-templates/${filename}`]),
      pretty(template)
    )
  }

  info(`New index creation`)
  const localTemplateIds = localTemplates.map((template) => template.id)
  const { value: newMappingsFor } = (await prompts({
    type: 'select',
    name: 'value',
    message: 'Do you need new mappings for:',
    choices: [
      {
        title: 'None',
        value: [],
      },
      {
        title: 'Both',
        value: localTemplates,
      },
      ...localTemplateIds.map((id) => ({
        title: id,
        value: [localTemplates.find((template) => template.id === id)],
      })),
    ],
  })) as { value: SearchTemplate[] }

  if (newMappingsFor.length > 0) {
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

    for (const template of newMappingsFor) {
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

      // Override the previous templates with new indices
      fs.writeFileSync(
        p([`../data/search-templates/${filename}`]),
        pretty({
          ...template,
          index,
        })
      )

      // We could have a prompt here to create and start the reindex,
      // but more often than not, you're going to want to change the mappings first
      success(
        `New config files created. Edit the mappings in ./data/indices/${filename}, and then run \n`
      )
      code(`  yarn createIndex --from ${index} --reindex \n`)
    }
  }
}

const argv = yargs(hideBin(process.argv))
  .options({
    name: { type: 'string', default: 'candidate' },
  })
  .parseSync()

go(argv)
