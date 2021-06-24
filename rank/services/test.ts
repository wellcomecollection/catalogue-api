import tests from '../data/tests'
import queries from '../data/queries'
import { Namespace } from '../types/namespace'
import { RankEvalResponse } from './elasticsearch'
import { asRankEvalRequestBody } from '../types/test'
import { TestResult } from '../pages/api/eval'
import { ParsedUrlQuery } from 'querystring'
import { decodeNamespace, Decoder, decodeString } from '../types/decoder'
import { getRankClient } from './elasticsearch-clients'

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
    label: test.label,
    description: test.description,
    namespace: 'works',
    pass: results.every((result) => result.result.pass),
    results,
  }
}

export const decoder: Decoder<Props> = (q: ParsedUrlQuery) => ({
  index: decodeString(q, 'index'),
  testId: decodeString(q, 'testId'),
  queryId: decodeString(q, 'queryId'),
  namespace: decodeNamespace(q.namespace),
})

function decode(query: ParsedUrlQuery): Props | undefined {
  try {
    return decoder(query)
  } catch (err) {
    return undefined
  }
}

export default service
export type { Props }
