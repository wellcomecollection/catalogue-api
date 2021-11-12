import {
  Namespace,
  QueryEnv,
  SearchTemplate,
  getNamespaceFromIndexName,
  namespaces as possibleNamespaces,
  queryEnvs,
} from '../types/searchTemplate'
import { gatherArgs, info, pretty } from '../scripts/utils'
import { images as imageTests, works as workTests } from '../data/tests'

import { TestResult } from '../types/test'
import { getTemplates } from '../services/search-templates'
import service from '../services/test'

global.fetch = require('node-fetch')

let searchTemplates: SearchTemplate[]
let queryEnv: QueryEnv
let namespaces: Namespace[]
let testIds: string[]

beforeAll(async () => {
  searchTemplates = await getTemplates()

  const possibleTestIds = workTests
    .map(({ id }) => id)
    .concat(imageTests.map(({ id }) => id))
    .filter((v, i, a) => a.indexOf(v) === i)

  const args = await gatherArgs({
    queryEnv: { type: 'string', choices: queryEnvs },
    namespaces: { type: 'array', choices: possibleNamespaces },
    testIds: {
      type: 'array',
      choices: possibleTestIds,
      default: possibleTestIds,
    },
  })

  queryEnv = args.queryEnv as QueryEnv
  namespaces = args.namespaces as Namespace[]
  testIds = args.testIds as string[]
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

test.each(workTests)('works.$id', async ({ id }) => {
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

test.each(imageTests)('images.$id', async ({ id }) => {
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
