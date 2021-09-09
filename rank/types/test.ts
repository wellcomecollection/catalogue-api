import { Env, Query } from './searchTemplate'
import { Eval, Pass } from '../data/tests/pass'

import { Metric } from './elasticsearch'

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
    env: Env
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
