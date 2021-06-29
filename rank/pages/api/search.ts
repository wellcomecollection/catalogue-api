import { Namespace, namespaces } from '../../types/namespace'
import { NextApiRequest, NextApiResponse } from 'next'

import { SearchResponse } from '../../types/elasticsearch'
import { TestResult } from '../../types/test'
import { getRankClient } from '../../services/elasticsearch'
import ranks from '../../ranks'
import { runTests } from './eval'

export type ApiResponse = SearchResponse & {
  results: TestResult[]
}

const decoder = (req: NextApiRequest) => ({
  query: req.query.query ? req.query.query.toString() : undefined,
  namespace: req.query.namespace ? req.query.namespace : 'works',
  env: req.query.env ? req.query.env : 'prod',
})

export default async (
  req: NextApiRequest,
  res: NextApiResponse
): Promise<void> => {
  const { query, namespace, env } = decoder(req)
  const rank = ranks.find((r) => r.id === `${namespace}-${env}`)
  const template = await rank.searchTemplate()
  const tests = rank.tests()
  const resultsReq = runTests(tests, template)
  const searchReq = getRankClient()
    .searchTemplate<SearchResponse>({
      index: template.index,
      body: {
        explain: true,
        source: {
          ...template.source,
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
