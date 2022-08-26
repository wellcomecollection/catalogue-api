import { Index, QueryEnv } from '../types/searchTemplate'

import { Decoder } from './decoder'
import { ParsedUrlQuery } from 'querystring'
import { decodeString } from './decoder'
import { getRankClient } from './elasticsearch'
import { getTemplate } from './search-templates'
import { estypes } from '@elastic/elasticsearch'

type Props = {
  searchTerms: string
  queryEnv: QueryEnv
  index: Index
  explain: boolean
}

export const decoder: Decoder<Props> = (q: ParsedUrlQuery) => ({
  searchTerms: decodeString(q, 'query'),
  queryEnv: decodeString(q, 'queryEnv') as QueryEnv,
  index: decodeString(q, 'index') as Index,
  explain: q['explain'] === 'true',
})

async function service({
  queryEnv,
  index,
  searchTerms,
  explain,
}: Props): Promise<estypes.SearchTemplateResponse> {
  const template = await getTemplate(queryEnv, index)
  const searchResp = await getRankClient().searchTemplate({
    index: template.index,
    body: {
      explain,
      source: JSON.stringify({
        query: template.query,
        track_total_hits: true,
        highlight: {
          pre_tags: ['<span class="bg-yellow-200">'],
          post_tags: ['</span>'],
          fields: { '*': { number_of_fragments: 0 } },
        },
      }),
      params: { query: searchTerms, size: 100 },
    },
  })

  return searchResp
}

export default service
