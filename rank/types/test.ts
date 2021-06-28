import { Pass, PassFn } from '../data/tests/pass'

import { Env } from './env'
import { Metric } from './elasticsearch'
import { SearchTemplateSource } from '../services/search-templates'

export type TestCase = {
  query: string
  ratings: string[]
  description?: string
}

export type Test = {
  id: string
  label: string
  description: string
  pass: PassFn
  cases: TestCase[]
  metric: Metric
  searchTemplateAugmentation?: (
    test: Test,
    source: SearchTemplateSource
  ) => SearchTemplateSource
}


export type TestResult = {
  index: string
  env: Env
  label: string
  description: string
  pass: boolean
  namespace: string
  results: {
    query: string
    description?: string
    result: Pass
  }[]
}
