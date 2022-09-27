import { Eval, Pass } from '../tests/pass'
import { Query, QueryEnv } from './searchTemplate'

import { RankEvalRankEvalMetric as Metric } from '@elastic/elasticsearch/lib/api/types'

export type TestCase = {
  query: string
  ratings: string[]
  description?: string
  knownFailure?: true
}

export type Test = {
  id: string
  label: string
  description: string
  eval: Eval
  cases: TestCase[]
  metric: Metric
  searchTemplateAugmentation?: (test: Test, query: Query) => Query
}

export type TestResult = {
  index: string
  queryEnv: QueryEnv
  label: string
  description: string
  pass: boolean
  namespace: string
  results: {
    query: string
    description?: string
    knownFailure?: true
    result: Pass
  }[]
}
