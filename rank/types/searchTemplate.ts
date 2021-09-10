export const querySources = ['remote', 'local'] as const
export type QuerySource = typeof querySources[number]

export const namespaces = ['works', 'images'] as const
export type Namespace = typeof namespaces[number]

export type Query = string | unknown
export type Index = string
export type SearchTemplateString = `${QuerySource}/${Index}`

export class SearchTemplate {
  index: Index
  namespace: Namespace
  querySource: QuerySource
  query: Query
  string: SearchTemplateString

  constructor(querySource: QuerySource, index: Index, query: Query) {
    this.querySource = querySource
    this.index = index
    this.query = query
    this.namespace = getNamespaceFromIndexName(index)
    this.string = `${this.querySource}/${this.index}`
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
