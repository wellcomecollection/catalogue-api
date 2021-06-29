import fs from 'fs'
import prompts from 'prompts'
import { code, info, p, pretty, success } from './utils'
import {
  getRemoteTemplates,
  SearchTemplate,
} from '../services/search-templates'
import { getRankClient } from '../services/elasticsearch'
import { getNamespaceFromIndexName } from '../types/namespace'

global.fetch = require('node-fetch')

async function go() {
  const client = getRankClient()

  info(`fetching search templates`)
  const templates = await getRemoteTemplates('prod')
  const indices = templates.map((template) => template.index)

  info(`getting settings for index config for ${indices}`)
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

  for (const index of indices) {
    const mappings = mappingsRes[index].mappings
    const analysis = settingsRes[index].settings.index.analysis
    const indexConfig = pretty({
      mappings,
      settings: {
        index: {
          analysis,
        },
      },
    })
    const query = pretty(
      templates.find((template) => template.index === index).source.query
    )
    info(`writing index config to /data/indices/ for ${index}`)
    fs.writeFileSync(p([`../data/indices/${index}.json`]), indexConfig)

    info(`writing query to /data/indices/ for ${index}`)
    fs.writeFileSync(p([`../data/queries/${index}.json`]), query)

    const searchTemplate = pretty({
      id: index,
      index,
      namespace: getNamespaceFromIndexName(index),
      env: 'local',
      source: { query: {} },
    } as SearchTemplate)

    info(`writing search templates to /data/search-templates/${index}.json`)
    fs.writeFileSync(
      p([`../data/search-templates/${index}.json`]),
      searchTemplate
    )
  }

  const { value: newMappingsFor } = await prompts({
    type: 'select',
    name: 'value',
    message: 'Do you need new mappings for:',
    choices: [
      {
        title: 'None',
        value: undefined,
      },
      {
        title: 'Both',
        value: indices,
      },
      ...indices.map((index) => ({
        title: index,
        value: [index],
      })),
    ],
  })

  if (newMappingsFor) {
    const { value: postFix } = await prompts({
      type: 'text',
      name: 'value',
      message: 'Postfix your index name (no namespace):',
    })

    for (const index of newMappingsFor) {
      const name = `${getNamespaceFromIndexName(index)}-${postFix}`
      info(`copying data from ${index} to ${name}`)
      fs.copyFileSync(
        p([`../data/indices/${index}.json`]),
        p([`../data/indices/${name}.json`])
      )
      fs.copyFileSync(
        p([`../data/queries/${index}.json`]),
        p([`../data/queries/${name}.json`])
      )
      fs.copyFileSync(
        p([`../data/search-templates/${index}.json`]),
        p([`../data/search-templates/${name}.json`])
      )

      // We could have a prompt here to create and start the reindex,
      // but more often than not, you're going to want to change the mappings first
      success(
        `new config files created. Edit the mappings in /data/indices/${name}, and then run \n`
      )
      code(`  yarn createIndex --from ${name} --reindex \n`)
    }
  }
}
go()
