import { Metric } from './services/elasticsearch'
import { PassFn } from './data/tests/pass'
import { SearchTemplateSource } from './services/search-templates'
import { QueryValue } from './types/decoder'

type QueryType = 'works' | 'images'
type Env = 'prod' | 'stage'

type TestCase = {
  query: string
  ratings: string[]
  description?: string
}

type Test = {
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

const namespaces = ['images', 'works'] as const
type Namespace = typeof namespaces[number]

function isNamespace(v: QueryValue): v is Namespace {
  return v
    ? (namespaces as ReadonlyArray<string>).includes(v?.toString())
    : false
}

function getNamespace(v: QueryValue): Namespace {
  if (!isNamespace(v)) throw Error(`${v} is not a valid namespace`)
  return v
}

export { isNamespace, getNamespace }
export type { Namespace, QueryType, Env, TestCase, Test }
