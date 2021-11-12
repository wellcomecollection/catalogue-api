import {
  QueryEnv,
  SearchTemplate,
  SearchTemplateString,
  getNamespaceFromIndexName,
  queryEnvs,
} from '../types/searchTemplate'
import { getTemplates, listIndices } from '../services/search-templates'

import { TestResult } from '../types/test'
import service from '../services/test'
import tests from '../data/tests'
import yargs from 'yargs'

global.fetch = require('node-fetch')
const { works, images } = tests

let searchTemplates: SearchTemplate[]
beforeAll(async () => {
  searchTemplates = await getTemplates()
})

const { queryEnv } = yargs(process.argv)
  .options({
    queryEnv: { type: 'string', demandOption: true, choices: queryEnvs },
  })
  // Passing .exitProcess(false) means we get helpful error messages
  // from jest/yargs if the CLI parsing fails.
  //
  // Compare:
  //
  //      process.exit called with "1"
  //
  // and:
  //
  //      Missing required argument: queryEnv
  //
  .exitProcess(false)
  .parseSync()

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
