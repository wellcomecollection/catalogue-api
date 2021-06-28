import { error, p, pretty } from './utils'

import fs from 'fs'

global.fetch = require('node-fetch')

async function go() {
  const [namespace, queryName] = process.argv.slice(2)
  if (!namespace) {
    error('Please specify an namespace')
  }
  if (!queryName) {
    error('Please specify an queryName')
  }

  const query = await import(`../data/queries/${queryName}.json`)
    .then((mod) => mod.default)
    .catch(() =>
      error(
        `query file does not exist. Create one at /data/queries/${queryName}.json} for better results`
      )
    )

  const fileName =
    namespace === 'works'
      ? 'WorksMultiMatcherQuery.json'
      : 'ImagesMultiMatcherQuery.json'

  fs.writeFileSync(
    p([`../../search/src/test/resources/${fileName}`]),
    pretty(query)
  )
}
go()
