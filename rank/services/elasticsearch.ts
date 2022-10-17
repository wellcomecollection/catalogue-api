import { Client } from '@elastic/elasticsearch'
import { getSecret } from './secrets'
import { QueryEnv } from '../types/searchTemplate'
import { apiUrl } from './search-templates'

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

const pipelineClients = {} as Record<QueryEnv, Client>
export async function getPipelineClient(env: QueryEnv): Promise<Client> {
  const pipelineDate = await fetch(`${apiUrl(env)}/catalogue/v2/_elasticConfig`)
    .then((res) => res.json())
    .then((res) => res.worksIndex.split('-').slice(-3).join('-'))

  const secretPrefix = `elasticsearch/pipeline_storage_${pipelineDate}/`
  const protocol = await getSecret(secretPrefix + 'protocol')
  const host = await getSecret(secretPrefix + 'public_host')
  const port = await getSecret(secretPrefix + 'port')
  const username = await getSecret(secretPrefix + 'es_username')
  const password = await getSecret(secretPrefix + 'es_password')

  if (!pipelineClients[env]) {
    pipelineClients[env] = new Client({
      node: `${protocol}://${host}:${port}`,
      auth: { username, password },
    })
  }
  return pipelineClients[env]
}
