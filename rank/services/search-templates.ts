import { Env } from '../types/env'
import { getNamespaceFromIndexName, Namespace } from '../types/namespace'
import { getRankClient } from './elasticsearch'

export async function listIndices() {
  const { body } = await getRankClient().cat.indices({ h: ['index'] })
  const indices = body
    .split('\n')
    .filter(
      (index: string) => !index.startsWith('.') && !index.startsWith('metrics')
    )
    .filter(Boolean)

  return indices
}

export type SearchTemplateSource = { query: unknown }

export type SearchTemplate = {
  id: string
  index: string
  namespace: Namespace
  env: Env
  source: SearchTemplateSource
}

type ApiSearchTemplate = {
  id: string
  index: string
  query: string // this is a JSON string
}

type ApiSearchTemplateRes = {
  templates: ApiSearchTemplate[]
}

function getNamespace(template: ApiSearchTemplate): Namespace {
  return template.index.split('-')[0] as Namespace
}

const endpoints = {
  stage:
    'https://api-stage.wellcomecollection.org/catalogue/v2/search-templates.json',
  prod: 'https://api.wellcomecollection.org/catalogue/v2/search-templates.json',
}

export async function getRemoteTemplates(env: Env): Promise<SearchTemplate[]> {
  const res = await fetch(endpoints[env])
  const json: ApiSearchTemplateRes = await res.json()

  // The query is returned as a string from the API
  return json.templates.map((template) => {
    const namespace = getNamespaceFromIndexName(template.index)
    return {
      id: `${env}/${namespace}/${template.index}`,
      index: `${template.index}`,
      namespace,
      env,
      source: { query: JSON.parse(template.query) },
    }
  })
}

export async function getLocalTemplates(): Promise<SearchTemplate[]> {
  const ids = await listIndices()
  const queriesReq = ids.map(async (id) => {
    const searchTemplate = await import(`../data/search-templates/${id}.json`)
      .then((m) => m.default)
      .catch(() => {})

    return searchTemplate
  })

  const queries: SearchTemplate[] = await Promise.all(queriesReq)
  return queries.filter(Boolean)
}

/**
 * This service merges remote and local search templates.
 *
 * A local search template is available when there is an existing index
 * with a corresponding query in `./data/queries/{index}.json`.
 */
export async function getTemplates(): Promise<SearchTemplate[]> {
  const remoteTemplates = await getRemoteTemplates('prod')
  const localTemplates = await getLocalTemplates()
  const templates = localTemplates.concat(remoteTemplates)
  return templates
}

export default getTemplates
