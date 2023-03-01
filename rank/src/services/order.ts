import { TestCase, TestResult } from '../types/test'

import { getClient } from './test'
import { getTemplate } from './search-templates'
import tests from '../tests'

function checkResponseOrder(searchResponse, testCase: TestCase): boolean {
  const beforeHits = searchResponse.hits.hits.filter((hit) =>
    testCase.before.includes(hit._id)
  )

  const afterHits = searchResponse.hits.hits.filter((hit) =>
    testCase.after.includes(hit._id)
  )

  const pass = beforeHits.every((beforeHit) => {
    return afterHits.every((afterHit) => {
      return beforeHit._score > afterHit._score
    })
  })

  return pass
}

async function testOrder({
  queryEnv,
  cluster,
  index,
  testId
}): Promise<TestResult> {
  // get the test data
  const template = await getTemplate(queryEnv, index)

  const test = tests[template.namespace].find((test) => test.id === testId)
  if (!test) throw Error(`No such test ${testId}`)

  const client = await getClient(queryEnv, cluster, template)

  const results = await Promise.all(
    // for each test case, run a search
    test.cases.map(async (testCase) => {
      const { searchTerms, description, knownFailure } = testCase

      // run the search for the test case and return all results
      const searchResponse = await client.searchTemplate({
        index: template.index,
        body: {
          source: JSON.stringify({
            query: template.query,
            track_total_hits: true,
            size: 10000
          }),
          params: { query: testCase.searchTerms }
        }
      })

      const pass = checkResponseOrder(searchResponse, testCase)

      return {
        searchTerms,
        description,
        knownFailure,
        result: {
          pass,
          score: pass ? 1 : 0
        }
      }
    })
  )

  return {
    index,
    queryEnv,
    pass: results.every((result) => result.result.pass),
    namespace: template.namespace,
    results
  }
}

export default testOrder
