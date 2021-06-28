import { error, info } from './utils'

import { rankClient } from '../services/elasticsearch'

async function go() {
  const [indexName] = process.argv.slice(2)
  if (!indexName) {
    error(
      'Please specifiy an `indexName` e.g. yarn deleteIndex works-with-secret-sauce'
    )
  }

  info(`deleting index ${indexName}`)
  const { body: deleteIndexRes } = await rankClient.indices
    .delete({
      index: indexName,
    })
    .catch((err) => {
      error(err.meta.body)
      return err
    })
  console.info(deleteIndexRes)
}

go()
