import { Env, Index } from '../types/searchTemplate'

import { Decoder } from '../types/decoder'
import { ParsedUrlQuery } from 'querystring'
import { SearchResponse } from '../types/elasticsearch'
import { decodeString } from './decoder'
import { getRankClient } from './elasticsearch'
import { getTemplate } from './search-templates'

type Props = {
  query: string
  env: Env
  index: Index
}

export const decoder: Decoder<Props> = (q: ParsedUrlQuery) => ({
  query: decodeString(q, 'query'),
  env: decodeString(q, 'env') as Env,
  index: decodeString(q, 'index') as Index,
})

async function service({ env, index, query }: Props): Promise<SearchResponse> {
  const template = await getTemplate(env, index)
  const searchResp = await getRankClient()
    .searchTemplate<SearchResponse>({
      index: template.index,
      body: {
        explain: true,
        source: {
          query: template.query,
          track_total_hits: true,
          highlight: {
            pre_tags: ['<span class="bg-yellow-200">'],
            post_tags: ['</span>'],
            fields: { '*': { number_of_fragments: 0 } },
          },
        },
        params: { query, size: 100 },
      },
    })
    .then((res) => res.body)

  return searchResp
}

export default service