import { error, info } from './utils'

import { namespaces } from '../types/namespace'
import { rankClient } from '../services/elasticsearch'

async function go() {
  const [indexName] = process.argv.slice(2)
  if (!indexName) {
    error(
      'Please specifiy an `indexName` e.g. yarn createIndex works-with-secret-sauce'
    )
  }

  if (!namespaces.some((n) => indexName.startsWith(n))) {
    error(`Make sure your indexName starts with any of these - ${namespaces}`)
  }

  const indexConfig = await import(`../data/indices/${indexName}.json`)
    .then((mod) => mod.default)
    .catch(() =>
      error(
        `index config file does not exist. Create one at /data/indices/${indexName}.json} for better results`
      )
    )

  info(`creating index ${indexName}`)
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

  if (putIndexRes.acknowledged) info(`created index ${indexName}`)
  if (!putIndexRes.acknowledged) {
    error(`couldn't create ${indexName} with error`)
    console.info(putIndexRes)
  }
}

go()
