import { TestCase, TestResult } from '../types/test'
import { decodeEnv, decodeNamespace, decodeString } from './decoder'

import { Decoder } from '../types/decoder'
import { Env } from '../types/env'
import { Namespace } from '../types/namespace'
import { ParsedUrlQuery } from 'querystring'
import { RankEvalResponse } from '../types/elasticsearch'
import { getRankClient } from './elasticsearch'
import getTemplates from './search-templates'
import tests from '../data/tests'

type Props = {
  testId: string
  env: Env
  index: string
  namespace: Namespace
}

/**
 * This service takes fetches a test from a `namespace`
 * looks up the query from our local or remote search-templates
 * and generates results from a rank_eval requst
 */
async function service({
  namespace,
  testId,
  env,
  index,
}: Props): Promise<TestResult> {
  const test = tests[namespace].find((test) => test.id === testId)
  if (!test) throw Error(`No such test ${testId}`)

  const templates = await getTemplates()
  const template = templates.find(
    (template) => template.id === index && template.env === env
  )
  const { cases, metric, searchTemplateAugmentation } = test

  const searchTemplate = searchTemplateAugmentation
    ? searchTemplateAugmentation(test, { ...template.source })
    : { ...template.source }

  const requests = cases.map((testCase: TestCase) => {
    return {
      id: testCase.query,
      template_id: template.id,
      params: {
        query: testCase.query,
      },
      ratings: testCase.ratings.map((id) => {
        return {
          _id: id,
          _index: index.id,
          rating: 3,
        }
      }),
    }
  })

  const reqBody = {
    requests,
    metric,
    templates: [{ id: template.id, template: { inline: searchTemplate } }],
  }

  const { body: rankEvalRes } =
    await getRankClient().rankEval<RankEvalResponse>({
      index,
      body: reqBody,
    })

  const results = Object.entries(rankEvalRes.details).map(([query, detail]) => {
    return {
      query,
      description: test.cases.find((c) => c.query === query)?.description,
      result: test.pass(detail),
    }
  })

  return {
    index,
    env,
    label: test.label,
    description: test.description,
    namespace: template.namespace,
    pass: results.every((result) => result.result.pass),
    results,
  }
}

export const decoder: Decoder<Props> = (q: ParsedUrlQuery) => ({
  testId: decodeString(q, 'testId'),
  index: decodeString(q, 'index'),
  env: decodeEnv(q.env),
  namespace: decodeNamespace(q.namespace),
})

export default service
export type { Props }
