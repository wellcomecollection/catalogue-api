import { Env, Namespace } from '../types'
import listIndicesService from './list-indices'

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

async function getSearchTemplates(env: Env): Promise<SearchTemplate[]> {
  const res = await fetch(endpoints[env])
  const json: ApiSearchTemplateRes = await res.json()

  // The query is returned as a string from the API
  return json.templates.map((template) => ({
    id: template.id,
    index: `ccr--${template.index}`,
    namespace: getNamespace(template),
    env,
    source: {
      query: JSON.parse(template.query),
    },
  }))
}

async function getLocalTemplates(ids: string[]): Promise<SearchTemplate[]> {
  const queriesReq = ids.map((id) =>
    import(`../data/queries/${id}.json`).then((m) => m.default).catch(() => {})
  )

  const queries = await Promise.all(queriesReq)

  return ids
    .filter((id, i) => queries[i])
    .map((id, i) => {
      return {
        id: id,
        index: id,
        namespace: id.replace('ccr--', '').split('-')[0] as Namespace,
        env: 'local',
        source: queries[i],
      }
    })
}

/**
 * This service merges remote and local search templates.
 *
 * A local search template is available when there is an existing index
 * with a correspondiny query in `./data/queries/{index}.json`.
 */
async function service(): Promise<SearchTemplate[]> {
  const remoteTemplates = await getSearchTemplates('prod')
  const indices = await listIndicesService()
  const localTemplates = await getLocalTemplates(indices)

  return localTemplates.concat(remoteTemplates)
}

export default service
export { getSearchTemplates }
