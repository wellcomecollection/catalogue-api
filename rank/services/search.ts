import { Decoder, decodeString } from './decoder'
import { Index, QueryEnv } from '../types/searchTemplate'

import { ParsedUrlQuery } from 'querystring'
import { estypes } from '@elastic/elasticsearch'
import { getRankClient } from './elasticsearch'
import { getTemplate } from './search-templates'

type Props = {
  searchTerms: string
  queryEnv: QueryEnv
  index: Index
}

export const decoder: Decoder<Props> = (q: ParsedUrlQuery) => ({
  searchTerms: decodeString(q, 'query'),
  queryEnv: decodeString(q, 'queryEnv') as QueryEnv,
  index: decodeString(q, 'index') as Index,
})

async function service({
  queryEnv,
  index,
  searchTerms,
}: Props): Promise<estypes.SearchTemplateResponse<Record<string, any>>> {
  const template = await getTemplate(queryEnv, index)
  const client = await getRankClient()
  return await client.searchTemplate({
    index: template.index,
    body: {
      source: JSON.stringify({
        query: template.query,
        track_total_hits: true,
      }),
      params: { query: searchTerms, size: 100 },
    },
  })
}

export default service
