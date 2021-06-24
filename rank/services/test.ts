import tests from '../data/tests'
import { Namespace } from '../types/namespace'
import { RankEvalResponse } from './elasticsearch'
import { asRankEvalRequestBody } from '../types/test'
import { TestResult } from '../pages/api/eval'
import { ParsedUrlQuery } from 'querystring'
import { decodeNamespace, Decoder, decodeString } from '../types/decoder'
import { getRankClient } from './elasticsearch-clients'
import { Env, isEnv } from '../types/env'
import searchTemplatesService from './search-templates'
import { addListener } from 'process'

type EnvIndex = {
  env: Env
  id: string
}

type Props = {
  testId: string
  index: EnvIndex
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
  index,
}: Props): Promise<TestResult> {
  const test = tests[namespace].find((test) => test.id === testId)
  if (!test) throw Error(`No such test ${testId}`)

  const templates = await searchTemplatesService()
  const template = templates.find(
    (template) => template.id === index.id && template.env === index.env
  )

  const reqBody = asRankEvalRequestBody(
    test,
    template.id,
    template.source,
    index.id
  )

  const { body: rankEvalRes } =
    await getRankClient().rankEval<RankEvalResponse>({
      index: index.id,
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
    namespace: template.namespace,
    pass: results.every((result) => result.result.pass),
    results,
  }
}

const decodeEnvIndex = (q: ParsedUrlQuery) => {
  if (!q.index) throw new Error('Missing value for queryId')
  const val = q.index.toString()
  const [env, id] = val.split(':')

  if (!env || !id) throw new Error(`${val} is not a valid EnvIndex`)
  if (!isEnv(env)) throw new Error(`${env} is not a valid Env`)

  return {
    env,
    id,
  }
}

export const decoder: Decoder<Props> = (q: ParsedUrlQuery) => ({
  testId: decodeString(q, 'testId'),
  index: decodeEnvIndex(q),
  namespace: decodeNamespace(q.namespace),
})

export default service
export type { Props }
