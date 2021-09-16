export const envs = ['remote', 'local'] as const
export type Env = typeof envs[number]

export const namespaces = ['works', 'images'] as const
export type Namespace = typeof namespaces[number]

export type Query = string | unknown
export type Index = string
export type SearchTemplateString = `${Env}/${Index}`

export class SearchTemplate {
  index: Index
  namespace: Namespace
  env: Env
  query: Query
  id: SearchTemplateString

  constructor(env: Env, index: Index, query: Query) {
    this.env = env
    this.index = index
    this.query = query
    this.namespace = getNamespaceFromIndexName(index)
    this.id = `${this.env}/${this.index}`
  }
}

export type ApiSearchTemplateRes = {
  templates: {
    id: string
    index: Index
    query: string // this is a JSON string
  }[]
}

export function getNamespaceFromIndexName(
  index: Index,
  fallback: Namespace = 'works'
): Namespace | undefined {
  // we prefix our index names with ccr--
  const noCcr = index.replace('ccr--', '')
  return noCcr.startsWith('works')
    ? 'works'
    : noCcr.startsWith('images')
    ? 'images'
    : fallback
}
