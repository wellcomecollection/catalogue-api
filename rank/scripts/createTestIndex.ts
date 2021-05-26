import { rankClient } from '../services/elasticsearch'

async function go() {
  const [indexName] = process.argv.slice(2)
  if (!indexName) {
    throw new Error(
      'Please specifiy an `indexName` e.g. yarn createTestIndex works-with-secret-sauce'
    )
  }

  const indexConfig = await import(`../data/indices/${indexName}`).then(
    (mod) => mod.default
  )
  const { body: putIndexRes } = await rankClient.indices
    .create({
      index: indexName,
      body: {
        ...indexConfig,
        settings: {
          ...indexConfig.settings,
          index: {
            ...indexConfig.settings.index,
            number_of_shards: 1,
            number_of_replicas: 0,
          },
        },
      },
    })
    .catch((err) => {
      console.error(err.meta.body)
      return err
    })
  console.info(putIndexRes)
}

go()
