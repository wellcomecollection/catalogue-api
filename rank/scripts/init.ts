import { info, p, pretty } from './utils'

import fs from 'fs'
import { getRemoteTemplates } from '../services/search-templates'
import { rankClient } from '../services/elasticsearch'

global.fetch = require('node-fetch')

async function go() {
  info('fetching search templates')
  const templates = await getRemoteTemplates('prod')
  const indices = templates.map((template) => template.index)

  info(`getting settings for ${indices}`)
  const settingsReq = rankClient.indices.getSettings({
    index: indices,
  })

  info(`getting mappings for ${indices}`)
  const mappingsReq = rankClient.indices.getMapping({
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
    info(`writing index config to /data/indices/ for ${indices}`)
    fs.writeFileSync(p([`../data/indices/${index}.json`]), indexConfig)

    info(`writing queries to /data/indices/ for ${indices}`)
    fs.writeFileSync(p([`../data/queries/${index}.json`]), query)
  }
}
go()
