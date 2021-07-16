import { ParsedUrlQuery } from 'querystring'
import { Decoder } from '../types/decoder'
import { Env } from '../types/env'
import { getNamespaceFromIndexName, Namespace } from '../types/namespace'
import { decodeString } from './decoder'
import { getRankClient } from './elasticsearch'
import worksProdQuery from '../public/WorksMultiMatcherQuery.json'

export type NamespacedIndex = {
  namespace: Namespace
  index: string
}
export async function listIndices(): Promise<NamespacedIndex[]> {
  const { body } = await getRankClient().cat.indices({ h: ['index'] })
  const indices = body
    .split('\n')
    .filter(
      (index: string) => !index.startsWith('.') && !index.startsWith('metrics')
    )
    .filter(Boolean)

  return indices.map((index: string) => ({
    namespace: getNamespaceFromIndexName(index),
    index,
  }))
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

const endpoints = {
  stage:
    'https://api-stage.wellcomecollection.org/catalogue/v2/search-templates.json',
  prod: 'https://api.wellcomecollection.org/catalogue/v2/search-templates.json',
}

let remoteTemplates: SearchTemplate[] | undefined
export async function getRemoteTemplates(env: Env): Promise<SearchTemplate[]> {
  if (!remoteTemplates) {
    const res = await fetch(endpoints[env])
    const json: ApiSearchTemplateRes = await res.json()

    // The query is returned as a string from the API
    remoteTemplates = json.templates.map((template) => {
      const namespace = getNamespaceFromIndexName(template.index)
      return {
        id: `${namespace}/${env}/${template.index}`,
        index: `${template.index}`,
        namespace,
        env,
        source: { query: JSON.parse(template.query) },
      }
    })
  }
  return remoteTemplates
}

export async function getLocalTemplates(): Promise<SearchTemplate[]> {
  const { NODE_ENV } = process.env
  const searchTemplates = await getRemoteTemplates('prod')
  const imports =
    NODE_ENV === 'development'
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

  return searchTemplates.map((template) => ({
    ...template,
    id: `${template.namespace}/local/${template.index}`,
    index: template.index,
    env: 'local',
    source: { query: queries[template.namespace] },
  }))
}

/**
 * This service merges remote and local search templates.
 *
 * A local search template is available when there is an existing index
 * with a corresponding query in `./data/queries/{index}.json`.
 */
type Props = {
  prod?: true
  local?: true
  test?: string[]
}
export async function getTemplates({ prod, local, test }: Props = {}): Promise<
  SearchTemplate[]
> {
  const remoteTemplates = prod ? await getRemoteTemplates('prod') : []
  const localTemplates = local ? await getLocalTemplates() : []
  const templates = remoteTemplates.concat(localTemplates)
  return templates
}

export default getTemplates
