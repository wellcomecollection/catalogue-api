import {
  ApiSearchTemplateRes,
  Index,
  QueryEnv,
  SearchTemplate,
  getNamespaceFromIndexName,
} from '../types/searchTemplate'

import { Client } from '@elastic/elasticsearch'

export const apiUrl = (queryEnv: QueryEnv): string => {
  if (queryEnv === 'production') {
    return 'https://api.wellcomecollection.org'
  }
  if (queryEnv === 'staging') {
    return 'https://api-stage.wellcomecollection.org'
  }
  throw new Error(`No API exists for environment ${queryEnv}!`)
}

export async function listIndices(client: Client): Promise<Index[]> {
  const body = await client.cat.indices({ h: ['index'] })
  const indices = (body as unknown as string) // The types are wrong here
    .split('\n')
    .filter(
      (index: string) => !index.startsWith('.') && !index.startsWith('metrics')
    )
    .filter(Boolean)
  return indices
}

async function getEnvironmentQueries(env: QueryEnv) {
  const res = await fetch(`${apiUrl(env)}/catalogue/v2/search-templates.json`)
  const json: ApiSearchTemplateRes = await res.json()
  const queries = Object.fromEntries(
    json.templates.map((template) => {
      const namespace = getNamespaceFromIndexName(template.index)
      const query = JSON.parse(template.query)
      return [namespace, query]
    })
  )
  return queries
}

export async function getCandidateQueries() {
  const imports = [
    import('../queries/WorksMultiMatcherQuery.json'),
    import('../queries/ImagesMultiMatcherQuery.json'),
  ]

  const queries = await Promise.all(imports).then(([works, images]) => ({
    works: works.default,
    images: images.default,
  }))
  return queries
}

async function getQueryFor(queryEnv: QueryEnv) {
  switch (queryEnv) {
    case 'candidate':
      return await getCandidateQueries()
    case 'production':
      return await getEnvironmentQueries('production')
    case 'staging':
      return await getEnvironmentQueries('staging')
  }
}

export async function getQueries() {
  const queries = {
    candidate: await getQueryFor('candidate'),
    production: await getQueryFor('production'),
    staging: await getQueryFor('staging'),
  }
  return queries
}

export async function getTemplate(
  queryEnv: QueryEnv,
  index: Index
): Promise<SearchTemplate> {
  const query = await getQueryFor(queryEnv)
  const namespacedQuery = query[getNamespaceFromIndexName(index)]
  return new SearchTemplate(queryEnv, index, namespacedQuery)
}
