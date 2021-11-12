import {
  Namespace,
  QueryEnv,
  SearchTemplate,
  getNamespaceFromIndexName,
  namespaces,
  queryEnvs,
} from '../types/searchTemplate'

import { TestResult } from '../types/test'
import { gatherArgs } from '../scripts/utils'
import { getTemplates } from '../services/search-templates'
import service from '../services/test'
import tests from '../data/tests'

global.fetch = require('node-fetch')
const { works, images } = tests

let searchTemplates: SearchTemplate[]
let queryEnv: QueryEnv
let namespace: Namespace
let testId: string

const testIds = tests.works
  .map(({ id }) => id)
  .concat(tests.images.map(({ id }) => id))
  .filter((v, i, a) => a.indexOf(v) === i)

beforeAll(async () => {
  searchTemplates = await getTemplates()

  const args = await gatherArgs({
    queryEnv: { type: 'string', choices: queryEnvs },
    namespace: { type: 'string', choices: namespaces },
    testId: { type: 'string', choices: testIds },
  })

  queryEnv = args.queryEnv as QueryEnv
  namespace = args.namespace as Namespace
  testId = args.testId as string
})

declare global {
  namespace jest {
    interface Matchers<R> {
      toPass(): CustomMatcherResult
    }
  }
}
expect.extend({
  toPass(result: TestResult['results'][number]) {
    if (result.knownFailure) {
      return {
        message: () => {
          return `"${result.query}" is a known failure`
        },
        pass: true,
      }
    }
    if (result.result.pass) {
      return {
        message: () => `"${result.query}" passes`,
        pass: true,
      }
    } else {
      return {
        message: () => `"${result.query}" fails`,
        pass: false,
      }
    }
  },
})

test.each(works)('works.$id', async ({ id }) => {
  const template = searchTemplates.find(
    (template) =>
      getNamespaceFromIndexName(template.index) === 'works' &&
      template.queryEnv === queryEnv
  )

  const result = await service({
    queryEnv: template.queryEnv,
    index: template.index,
    testId: id,
  })

  result.results.forEach((result) => {
    expect(result).toPass()
  })
})

test.each(images)('images.$id', async ({ id }) => {
  const template = searchTemplates.find(
    (template) =>
      getNamespaceFromIndexName(template.index) === 'images' &&
      template.queryEnv === queryEnv
  )

  const result = await service({
    queryEnv: template.queryEnv,
    index: template.index,
    testId: id,
  })

  result.results.forEach((result) => {
    expect(result).toPass()
  })
})
