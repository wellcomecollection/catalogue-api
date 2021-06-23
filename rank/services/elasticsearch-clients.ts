import { Client } from '@elastic/elasticsearch'

const { ES_RANK_USER, ES_RANK_PASSWORD, ES_RANK_CLOUD_ID } = process.env

let rankClient
function getRankClient(): Client {
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

export { getRankClient }
