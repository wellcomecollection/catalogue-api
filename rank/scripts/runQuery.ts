import { Client } from '@elastic/elasticsearch'
import { aggs, sort } from '../data/queries/common'

const { ES_CLOUD_ID, ES_PASSWORD, ES_USER } = process.env

const catalogueClient = new Client({
  cloud: {
    id: ES_CLOUD_ID,
  },
  auth: {
    username: ES_USER,
    password: ES_PASSWORD,
  },
})

async function go() {
  const [queryName, index, timesString] = process.argv.slice(2)
  const times = parseInt(timesString, 10)
  const query = await import(`../data/queries/${queryName}`).then(
    (m) => m.default
  )

  const range = Array(times)
    .fill(0)
    .map((_, i) => i * i)
  range.forEach(async () => {
    const fullQuery = {
      query,
      aggs,
      sort,
    }
    const { body } = await catalogueClient
      .searchTemplate({
        index,
        body: { source: fullQuery },
      })
      .catch((err) => {
        console.info(err.meta.body)
        return { body: err }
      })

    console.info(body.took)
  })
}

go()
