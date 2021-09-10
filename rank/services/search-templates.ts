import {
  ApiSearchTemplateRes,
  Env,
  Index,
  SearchTemplate,
  SearchTemplateString,
  envs,
  getNamespaceFromIndexName,
} from '../types/searchTemplate'

import { getRankClient } from './elasticsearch'

export async function listIndices(): Promise<Index[]> {
  const { body } = await getRankClient().cat.indices({ h: ['index'] })
  const indices = body
    .split('\n')
    .filter(
      (index: string) => !index.startsWith('.') && !index.startsWith('metrics')
    )
    .filter(Boolean)
  return indices
}

export async function getRemoteQueries() {
  const res = await fetch(
    'https://api.wellcomecollection.org/catalogue/v2/search-templates.json'
  )
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

export async function getLocalQueries() {
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
          import('../public/WorksMultiMatcherQuery.json'),
          import('../public/ImagesMultiMatcherQuery.json'),
        ]

  const queries = await Promise.all(imports).then(([works, images]) => ({
    works: works.default,
    images: images.default,
  }))
  return queries
}

export async function getQueries() {
  const queries = {
    local: await getLocalQueries(),
    remote: await getRemoteQueries(),
  }
  return queries
}

export async function getTemplates(
  filterIds?: SearchTemplateString[]
): Promise<SearchTemplate[]> {
  const indices = await listIndices()
  const ids =
    filterIds ??
    envs.flatMap((env) =>
      indices.map((index) => `${env}/${index}` as SearchTemplateString)
    )

  const queries = await getQueries()
  const templates = ids.map((id) => {
    const [env, index] = id.split('/')
    const query = queries[env][getNamespaceFromIndexName(index)]
    return new SearchTemplate(env as Env, index as Index, query)
  })

  return templates
}

export async function getTemplate(
  env: Env,
  index: Index
): Promise<SearchTemplate> {
  const queries = await getQueries()
  const query = queries[env][getNamespaceFromIndexName(index)]
  return new SearchTemplate(env, index, query)
}

export default getTemplates
