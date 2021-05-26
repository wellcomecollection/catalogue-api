import { rankClient } from '../services/elasticsearch'

async function go() {
  const [indexName] = process.argv.slice(2)
  if (!indexName) {
    throw new Error(
      'Please specifiy `indexName` e.g. yarn deleteTestIndex works-with-secret-sauce'
    )
  }

  const { body: deleteIndexRes } = await rankClient.indices
    .delete({
      index: indexName,
    })
    .catch((err) => {
      console.error(err.meta.body)
      return err
    })
  console.info(deleteIndexRes)
}

go()
