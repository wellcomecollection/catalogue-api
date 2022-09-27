import {
  ApiSearchTemplateRes,
  Index,
  QueryEnv,
  SearchTemplate,
  SearchTemplateString,
  getNamespaceFromIndexName,
  queryEnvs,
} from '../types/searchTemplate'

import { getRankClient } from './elasticsearch'

export async function listIndices(): Promise<Index[]> {
  const body = await getRankClient().cat.indices({ h: ['index'] })
  const indices = (body as unknown as string) // The types are wrong here
    .split('\n')
    .filter(
      (index: string) => !index.startsWith('.') && !index.startsWith('metrics')
    )
    .filter(Boolean)
  return indices
}

async function getEnvironmentQueries(env: QueryEnv) {
  const apiUrl = {
    production:
      'https://api.wellcomecollection.org/catalogue/v2/search-templates.json',
    staging:
      'https://api-stage.wellcomecollection.org/catalogue/v2/search-templates.json',
  }[env]

  const res = await fetch(apiUrl)
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
  const imports =
    process.env.NODE_ENV === 'development'
      ? [
          import('../../search/src/test/resources/WorksMultiMatcherQuery.json'),
          import(
            '../../search/src/test/resources/ImagesMultiMatcherQuery.json'
          ),
        ]
      : // These are copied over during build stage
        [
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

export async function getTemplates(
  filterIds?: SearchTemplateString[]
): Promise<SearchTemplate[]> {
  const indices = await listIndices()
  const ids =
    filterIds ??
    queryEnvs.flatMap((queryEnv) =>
      indices.map((index) => `${queryEnv}/${index}` as SearchTemplateString)
    )

  const queries = await getQueries()
  const templates = ids.map((id) => {
    const [queryEnv, index] = id.split('/')
    const query = queries[queryEnv][getNamespaceFromIndexName(index)]
    return new SearchTemplate(queryEnv as QueryEnv, index as Index, query)
  })

  return templates
}

export async function getTemplate(
  queryEnv: QueryEnv,
  index: Index
): Promise<SearchTemplate> {
  const query = await getQueryFor(queryEnv)
  const namespacedQuery = query[getNamespaceFromIndexName(index)]
  return new SearchTemplate(queryEnv, index, namespacedQuery)
}

export default getTemplates
