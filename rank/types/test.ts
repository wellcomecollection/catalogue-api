import { PassFn } from '../data/tests/pass'
import { Metric } from '../services/elasticsearch'
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

function asRankEvalRequestBody(
  test: Test,
  queryId: string,
  query: SearchTemplateSource,
  index: string
) {
  const { cases, metric, searchTemplateAugmentation } = test

  const searchTemplate = searchTemplateAugmentation
    ? searchTemplateAugmentation(test, { ...query })
    : { ...query }

  const requests = cases.map((testCase: TestCase) => {
    return {
      id: testCase.query,
      template_id: queryId,
      params: {
        query: testCase.query,
      },
      ratings: testCase.ratings.map((id) => {
        return {
          _id: id,
          _index: index,
          rating: 3,
        }
      }),
    }
  })

  const body = {
    requests,
    metric,
    templates: [{ id: queryId, template: { inline: searchTemplate } }],
  }

  return body
}

export { asRankEvalRequestBody }
