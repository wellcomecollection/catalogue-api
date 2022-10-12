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

let pipelineClient
export async function getPipelineClient(): Promise<Client> {
  const pipelineDate = await fetch(
    'https://api.wellcomecollection.org/catalogue/v2/_elasticConfig'
  )
    .then((res) => res.json())
    .then((res) => res.worksIndex.split('-').slice(-3).join('-'))

  const secretPrefix = `elasticsearch/pipeline_storage_${pipelineDate}/`
  const protocol = await getSecret(secretPrefix + 'protocol')
  const host = await getSecret(secretPrefix + 'public_host')
  const port = await getSecret(secretPrefix + 'port')
  const username = await getSecret(secretPrefix + 'es_username')
  const password = await getSecret(secretPrefix + 'es_password')

  if (!pipelineClient) {
    pipelineClient = new Client({
      node: `${protocol}://${host}:${port}`,
      auth: { username, password },
    })
  }
  return pipelineClient
}

export { rankClient, reportingClient, pipelineClient }
