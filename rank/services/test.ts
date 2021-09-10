import { Env, Index, SearchTemplate } from '../types/searchTemplate'
import { TestCase, TestResult } from '../types/test'

import { Decoder } from '../types/decoder'
import { ParsedUrlQuery } from 'querystring'
import { RankEvalResponse } from '../types/elasticsearch'
import { decodeString } from './decoder'
import { getRankClient } from './elasticsearch'
import { getTemplate } from './search-templates'
import tests from '../data/tests'

type Props = {
  testId: string
  env: Env
  index: Index
}

async function service({ env, index, testId }: Props): Promise<TestResult> {
  const notAugmentedTemplate = await getTemplate(env, index)

  const test = tests[notAugmentedTemplate.namespace].find(
    (test) => test.id === testId
  )
  if (!test) throw Error(`No such test ${testId}`)

  const { cases, metric, searchTemplateAugmentation } = test

  const template = searchTemplateAugmentation
    ? new SearchTemplate(
        env,
        index,
        searchTemplateAugmentation(test, notAugmentedTemplate.query)
      )
    : notAugmentedTemplate

  const requests = cases.map((testCase: TestCase) => {
    return {
      id: testCase.query,
      template_id: template.string,
      params: {
        query: testCase.query,
      },
      ratings: testCase.ratings.map((id) => {
        return {
          _id: id,
          _index: template.index,
          rating: 3,
        }
      }),
    }
  })

  const res = await getRankClient().rankEval<RankEvalResponse>({
    index: template.index,
    body: {
      requests,
      metric,
      templates: [
        {
          id: template.string,
          template: { source: { query: template.query } },
        },
      ],
    },
  })

  const results = Object.entries(res.body.details).map(([query, detail]) => {
    const testCase = test.cases.find((c) => c.query === query)
    return {
      query,
      description: testCase?.description,
      knownFailure: testCase?.knownFailure,
      result: test.eval(detail),
    }
  })

  return {
    index: template.index,
    env: template.env,
    label: test.label,
    description: test.description,
    namespace: template.namespace,
    pass: results.every((result) => result.result.pass),
    results,
  }
}

export const decoder: Decoder<Props> = (q: ParsedUrlQuery) => ({
  testId: decodeString(q, 'testId'),
  env: decodeString(q, 'env') as Env,
  index: decodeString(q, 'index') as Index,
})

export default service
export type { Props }
