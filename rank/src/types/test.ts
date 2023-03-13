import { Eval, Pass } from '../services/pass'

import { RankEvalRankEvalMetric as Metric } from '@elastic/elasticsearch/lib/api/types'
import { QueryEnv } from './searchTemplate'

export type TestCase = {
  searchTerms: string
  ratings?: string[]
  before?: string[]
  after?: string[]
  description?: string
  knownFailure?: boolean
  filter?: any
}

export type Test = {
  id: string
  description: string
  eval?: Eval
  cases: TestCase[]
  metric?: Metric
}

export type TestResult = {
  index: string
  queryEnv: QueryEnv
  description?: string
  pass: boolean
  namespace: string
  results: {
    searchTerms: string
    description?: string
    knownFailure?: boolean
    result: Pass
  }[]
}
