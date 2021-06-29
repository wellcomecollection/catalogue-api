import { Client } from '@elastic/elasticsearch'

const {
  ES_RANK_USER,
  ES_RANK_PASSWORD,
  ES_RANK_CLOUD_ID,
  ES_RATINGS_USER,
  ES_RATINGS_PASSWORD,
  ES_RATINGS_CLOUD_ID,
} = process.env

let rankClient
export function getRankClient(): Client {
  if (!rankClient) {
    rankClient = new Client({
      cloud: {
        id: ES_RANK_CLOUD_ID!,
      },
      auth: {
        username: ES_RANK_USER!,
        password: ES_RANK_PASSWORD!,
      },
    })
  }
  return rankClient
}

let ratingClient
export function getRatingClient(): Client {
  if (!ratingClient) {
    ratingClient = new Client({
      cloud: {
        id: ES_RATINGS_CLOUD_ID!,
      },
      auth: {
        username: ES_RATINGS_USER!,
        password: ES_RATINGS_PASSWORD!,
      },
    })
  }
  return ratingClient
}

export { rankClient, ratingClient }
