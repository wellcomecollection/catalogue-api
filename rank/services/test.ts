import tests from '../data/tests'
import queries from '../data/queries'
import { Namespace } from '../types'
import { rankClient, RankEvalResponse } from './elasticsearch'
import { asRankEvalRequestBody } from '../types/test'
import { TestResult } from '../pages/api/eval'

type Props = {
  index: string
  testId: string
  queryId: string
  namespace: Namespace
}

async function service({
  index,
  testId,
  queryId,
  namespace,
}: Props): Promise<TestResult> {
  const test = tests[namespace].find((test) => test.id === testId)

  if (!test) throw Error(`No such test ${testId}`)

  const query = queries[queryId]
  const reqBody = asRankEvalRequestBody(test, queryId, query, index)

  const { body: rankEvalRes } = await rankClient.rankEval<RankEvalResponse>({
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
    label: test.label,
    description: test.description,
    namespace: 'works',
    pass: results.every((result) => result.result.pass),
    results,
  }
}

export default service
export type { Props }
