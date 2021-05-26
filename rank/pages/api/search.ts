import { NextApiRequest, NextApiResponse } from 'next'
import { SearchResponse, rankClient } from '../../services/elasticsearch'
import { TestResult, runTests } from './eval'
import ranks from '../../ranks'
import { Namespace } from '../../types'

export type ApiResponse = SearchResponse & {
  results: TestResult[]
}

export type ApiRequest = {
  query?: string
  useTestQuery?: 'true' | 'false'
  namespace?: Namespace
}

type Q = NextApiRequest['query']
const decoder = (q: Q) => ({
  query: q.query ? q.query.toString() : undefined,
  rankId: q.rankId ? q.rankId.toString() : 'works-prod',
})

export default async (
  req: NextApiRequest,
  res: NextApiResponse
): Promise<void> => {
  const { query, rankId } = decoder(req.query)
  const rank = ranks.find((r) => r.id === rankId)
  const template = await rank.searchTemplate()
  const tests = rank.tests()
  const resultsReq = runTests(tests, template)
  const searchReq = rankClient
    .searchTemplate<SearchResponse>({
      index: template.index,
      body: {
        explain: true,
        source: {
          ...template.source,
          track_total_hits: true,
          highlight: {
            pre_tags: ['<em class="bg-yellow-200">'],
            post_tags: ['</em>'],
            fields: { '*': { number_of_fragments: 0 } },
          },
        },
        params: { query },
      },
    })
    .then((res) => res.body)

  const requests: [Promise<SearchResponse>, Promise<TestResult[]>] = [
    searchReq,
    Promise.all(resultsReq),
  ]

  const [searchResp, ...[results]] = await Promise.all(requests)
  const response: ApiResponse = {
    ...searchResp,
    results,
  }

  res.statusCode = 200
  res.setHeader('Content-Type', 'application/json')
  res.end(JSON.stringify(response))
}
