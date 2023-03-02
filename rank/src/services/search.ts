import { Index, QueryEnv } from '../types/searchTemplate'

import { estypes } from '@elastic/elasticsearch'
import { getRankClient } from './elasticsearch'
import { getTemplate } from './search-templates'

type Props = {
  searchTerms: string
  queryEnv: QueryEnv
  index: Index
  filter?: any
}

async function search({
  queryEnv,
  index,
  searchTerms,
  filter
}: Props): Promise<estypes.SearchTemplateResponse<Record<string, any>>> {
  const template = await getTemplate(queryEnv, index)
  const client = await getRankClient()
  return await client.searchTemplate({
    index: template.index,
    body: {
      source: JSON.stringify({
        query: template.query,
        track_total_hits: true,
        ...(filter ? { post_filter: JSON.parse(filter) } : {})
      }),
      params: { query: searchTerms, size: 100 }
    }
  })
}

export default search
