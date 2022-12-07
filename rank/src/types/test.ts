import { Eval, Pass } from '../services/pass'

import { RankEvalRankEvalMetric as Metric } from '@elastic/elasticsearch/lib/api/types'
import { QueryEnv } from './searchTemplate'

export type TestCase = {
  query: string
  ratings: string[]
  description?: string
  knownFailure?: boolean
}

export type Test = {
  id: string
  label: string
  description: string
  eval: Eval
  cases: TestCase[]
  metric: Metric
}

export type TestResult = {
  index: string
  queryEnv: QueryEnv
  label: string
  description?: string
  pass: boolean
  namespace: string
  results: {
    query: string
    description?: string
    knownFailure?: boolean
    result: Pass
  }[]
}
