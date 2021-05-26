import { rankClient } from '../services/elasticsearch'

async function go() {
  const [indexName] = process.argv.slice(2)
  if (!indexName) {
    throw new Error(
      'Please specifiy an `indexName` e.g. yarn updateTestIndex works-with-secret-sauce'
    )
  }

  const indexConfig = await import(`../data/indices/${indexName}`).then(
    (mod) => mod.default
  )

  const { body: closeIndedxRes } = await rankClient.indices.close({
    index: indexName,
  })
  console.info(closeIndedxRes)

  const { body: putSettingsRes } = await rankClient.indices
    .putSettings({
      index: indexName,
      body: {
        ...indexConfig.settings,
      },
    })
    .catch((err) => {
      console.error(err.meta.body)
      return err
    })
  console.info(putSettingsRes)

  const { body: putMappingRes } = await rankClient.indices
    .putMapping({
      index: indexName,
      body: {
        ...indexConfig.mappings,
      },
    })
    .catch((err) => {
      console.error(err.meta.body)
      return err
    })

  console.info(putMappingRes)

  const { body: openIndedxRes } = await rankClient.indices.open({
    index: indexName,
  })
  console.info(openIndedxRes)
}

go()
