import { error, p, pretty } from './utils'

import fs from 'fs'
import { getNamespaceFromIndexName } from '../types/namespace'
import { SearchTemplate } from '../services/search-templates'
import yargs from 'yargs/yargs'
import { hideBin } from 'yargs/helpers'

global.fetch = require('node-fetch')

async function go(args: typeof argv) {
  const { name } = args

  if (!name) {
    error('Please specify an queryName')
  }

  const template: SearchTemplate = await import(
    `../data/search-templates/${name}.json`
  )
    .then((mod) => mod.default)
    .catch(() =>
      error(
        `search template does not exist. Create one at ./data/search-templates/${name}.json} for better results`
      )
    )

  const namespace = getNamespaceFromIndexName(name)
  const fileName =
    namespace === 'works'
      ? 'WorksMultiMatcherQuery.json'
      : 'ImagesMultiMatcherQuery.json'

  fs.writeFileSync(
    p([`../../search/src/test/resources/${fileName}`]),
    pretty(template.source.query)
  )
}

const argv = yargs(hideBin(process.argv))
  .options({
    name: { type: 'string', default: 'candidate' },
  })
  .parseSync()

go(argv)
