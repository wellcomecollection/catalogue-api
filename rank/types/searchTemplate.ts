export const queryEnvs = ['production', 'candidate'] as const
export type QueryEnv = typeof queryEnvs[number]

export const namespaces = ['works', 'images'] as const
export type Namespace = typeof namespaces[number]

export type Query = string | unknown
export type Index = string
export type SearchTemplateString = `${QueryEnv}/${Index}`

export class SearchTemplate {
  index: Index
  namespace: Namespace
  queryEnv: QueryEnv
  query: Query
  id: SearchTemplateString

  constructor(queryEnv: QueryEnv, index: Index, query: Query) {
    this.queryEnv = queryEnv
    this.index = index
    this.query = query
    this.namespace = getNamespaceFromIndexName(index)
    this.id = `${this.queryEnv}/${this.index}`
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
