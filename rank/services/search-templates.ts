import { ParsedUrlQuery } from 'querystring'
import { Decoder } from '../types/decoder'
import { Env } from '../types/env'
import { getNamespaceFromIndexName, Namespace } from '../types/namespace'
import { decodeString } from './decoder'
import { getRankClient } from './elasticsearch'

export async function listIndices() {
  const { body } = await getRankClient().cat.indices({ h: ['index'] })
  const indices = body
    .split('\n')
    .filter(
      (index: string) => !index.startsWith('.') && !index.startsWith('metrics')
    )
    .filter(Boolean)

  return indices
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

function getNamespace(template: ApiSearchTemplate): Namespace {
  return template.index.split('-')[0] as Namespace
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
        id: `${env}/${namespace}/${template.index}`,
        index: `${template.index}`,
        namespace,
        env,
        source: { query: JSON.parse(template.query) },
      }
    })
  }
  return remoteTemplates
}

export async function getLocalTemplates({
  worksIndex,
  imagesIndex,
}: Props): Promise<SearchTemplate[]> {
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
    id: `local/${template.namespace}/${template.index}`,
    index:
      template.namespace === 'works' && worksIndex
        ? worksIndex
        : template.namespace === 'images' && imagesIndex
        ? imagesIndex
        : template.index,
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
export async function getTemplates({
  worksIndex,
  imagesIndex,
}: Props): Promise<SearchTemplate[]> {
  const remoteTemplates = await getRemoteTemplates('prod')
  const localTemplates = await getLocalTemplates({ worksIndex, imagesIndex })
  const templates = remoteTemplates.concat(localTemplates)
  return templates
}

export type Props = {
  worksIndex?: string
  imagesIndex?: string
}

export const decoder: Decoder<Props> = (q: ParsedUrlQuery) => ({
  worksIndex: q.worksIndex ? decodeString(q, 'worksIndex') : undefined,
  imagesIndex: q.imagesIndex ? decodeString(q, 'imagesIndex') : undefined,
})

export default getTemplates
