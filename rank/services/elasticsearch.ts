import { Client } from '@elastic/elasticsearch'
import { getSecret } from './secrets'

let rankClient
export async function getRankClient(): Promise<Client> {
  const id = await getSecret('elasticsearch/rank/ES_RANK_CLOUD_ID')
  const username = await getSecret('elasticsearch/rank/ES_RANK_USER')
  const password = await getSecret('elasticsearch/rank/ES_RANK_PASSWORD')

  if (!rankClient) {
    rankClient = new Client({
      cloud: { id },
      auth: { username, password },
    })
  }
  return rankClient
}

let reportingClient
export async function getReportingClient(): Promise<Client> {
  const id = await getSecret('elasticsearch/rank/ES_REPORTING_CLOUD_ID')
  const username = await getSecret('elasticsearch/rank/ES_REPORTING_USER')
  const password = await getSecret('elasticsearch/rank/ES_REPORTING_PASSWORD')

  if (!reportingClient) {
    reportingClient = new Client({
      cloud: { id },
      auth: { username, password },
    })
  }
  return reportingClient
}

export { rankClient, reportingClient }
