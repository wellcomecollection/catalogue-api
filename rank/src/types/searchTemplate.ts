export const queryEnvs = ['production', 'staging', 'candidate'] as const
export type QueryEnv = typeof queryEnvs[number]

export const clusters = ['pipeline', 'rank'] as const
export type Cluster = typeof clusters[number]

export const namespaces = ['works', 'images'] as const
export type Namespace = typeof namespaces[number]

export type Index = string
export type SearchTemplateString = `${QueryEnv}/${Index}`

export class SearchTemplate {
  index: Index
  namespace: Namespace
  queryEnv: QueryEnv
  query: string
  id: SearchTemplateString

  constructor(queryEnv: QueryEnv, index: Index, query: string) {
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
  return index.startsWith('works')
    ? 'works'
    : index.startsWith('images')
    ? 'images'
    : fallback
}
