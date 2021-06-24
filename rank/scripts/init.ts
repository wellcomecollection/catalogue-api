import { getRankClient } from '../services/elasticsearch-clients'
import { getSearchTemplates } from '../services/search-templates'
import fs from 'fs'
import { p, pretty } from './utils'
import chalk from 'chalk'

global.fetch = require('node-fetch')

const info = (message: string) => {
  console.log(chalk.blue(message))
}

async function go() {
  info('fetching search templates')
  const templates = await getSearchTemplates('prod')
  const client = getRankClient()
  const indices = templates.map((template) => template.index)

  info(`getting settings for ${indices}`)
  const settingsReq = client.indices.getSettings({
    index: indices,
  })

  info(`getting mappings for ${indices}`)
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
      templates.find((template) => template.index === index).source
    )
    info(`writing index config to /data/indices/ for ${indices}`)
    fs.writeFileSync(p([`../data/indices/${index}.json`]), indexConfig)

    info(`writing queries to /data/indices/ for ${indices}`)
    fs.writeFileSync(p([`../data/queries/${index}.json`]), query)
  }
}
go()
