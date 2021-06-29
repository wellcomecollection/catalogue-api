import tests from '../data/tests'
import {
  getRemoteTemplates,
  SearchTemplate,
} from '../services/search-templates'
import testService from '../services/test'
import { getNamespaceFromIndexName } from '../types/namespace'
import { TestResult } from '../types/test'

global.fetch = require('node-fetch')
const { works, images } = tests

let searchTemplates: SearchTemplate[]
beforeAll(async () => {
  searchTemplates = await getRemoteTemplates('prod')
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
          console.info(result.query)
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
    (template) => getNamespaceFromIndexName(template.index) === 'works'
  )

  const result = await testService({
    testId: id,
    templateId: template.id,
  })

  result.results.forEach((result) => {
    expect(result).toPass()
  })
})

test.each(images)('images.$id', async ({ id }) => {
  const template = searchTemplates.find(
    (template) => getNamespaceFromIndexName(template.index) === 'images'
  )

  const result = await testService({
    testId: id,
    templateId: template.id,
  })

  result.results.forEach((result) => {
    expect(result).toPass()
  })
})
