import { Env, Namespace } from '../types'

export type SearchTemplateSource = { query: unknown }
export type SearchTemplate = {
  id: string
  index: string
  namespace: Namespace
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
    source: {
      query: JSON.parse(template.query),
    },
  }))
}

export { getSearchTemplates }
