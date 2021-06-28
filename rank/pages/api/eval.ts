import { NextApiRequest, NextApiResponse } from 'next'
import {
  RankEvalRequestRequest,
  RankEvalResponse,
} from '../../types/elasticsearch'
import {
  SearchTemplate,
  getRemoteTemplates,
} from '../../services/search-templates'
import { Test, TestCase, TestResult } from '../../types/test'

import { Env } from '../../types/env'
import { getRankClient } from '../../services/elasticsearch'
import tests from '../../data/tests'

function casesToRankEvalRequest(
  cases: TestCase[],
  template: SearchTemplate
): RankEvalRequestRequest<{ query: string }>[] {
  return cases.map((example: TestCase) => {
    return {
      id: example.query,
      template_id: template.id,
      params: {
        query: example.query,
      },
      ratings: example.ratings.map((id) => {
        return {
          _id: id,
          _index: template.index,
          rating: 3,
        }
      }),
    }
  })
}

export function rankEvalRequest(
  template: SearchTemplate,
  test: Test
): Promise<RankEvalResponse> {
  const { id, index } = template
  const { cases, metric, searchTemplateAugmentation } = test
  const requests = casesToRankEvalRequest(cases, template)
  const searchTemplate = searchTemplateAugmentation
    ? searchTemplateAugmentation(test, template.source)
    : template.source

  const body = {
    requests,
    metric,
    templates: [{ id, template: { source: searchTemplate } }],
  }

  const req = getRankClient()
    .rankEval<RankEvalResponse>({ index, body })
    .then((res) => res.body)

  return req
}

export function runTests(
  tests: Test[],
  template: SearchTemplate
): Promise<TestResult>[] {
  const testResults = tests.map((test) =>
    rankEvalRequest(template, test).then((res) => {
      const results = Object.entries(res.details).map(([query, detail]) => {
        return {
          query,
          description: test.cases.find((c) => c.query === query).description,
          result: test.eval(detail),
        }
      })
      return {
        env: template.env,
        index: template.id,
        label: test.label,
        description: test.description,
        namespace: template.namespace,
        pass: results.every((result) => result.result.pass),
        results,
      }
    })
  )

  return testResults
}

export type ApiResponse = {
  results: TestResult[]
}

export default async (
  req: NextApiRequest,
  res: NextApiResponse
): Promise<void> => {
  const env = req.query.env ? req.query.env : 'prod'
  const searchTemplates = await getRemoteTemplates(env as Env)

  // We run multiple tests with different metrics against different indexes
  // see: https://github.com/elastic/elasticsearch/issues/51680
  const results: TestResult[] = await Promise.all(
    searchTemplates
      .map((template) => runTests(tests[template.namespace], template))
      .reduce((cur, acc) => cur.concat(acc), [])
  )

  res.statusCode = 200
  res.setHeader('Content-Type', 'application/json')
  res.end(JSON.stringify({ results }))
}
