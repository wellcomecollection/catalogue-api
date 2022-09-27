import { Index, QueryEnv } from '../types/searchTemplate'

import { TestResult } from '../types/test'
import service from '../services/test'
import yargs from 'yargs'

global.fetch = require('node-fetch')

const args = yargs(process.argv)
  .options({
    queryEnv: { type: 'string', demandOption: true },
    index: { type: 'string', demandOption: true },
    testId: { type: 'array', demandOption: true },
  })
  .exitProcess(false)
  .parseSync()

const queryEnv = args.queryEnv as QueryEnv
const index = args.index as Index
const testIds = args.testId as string[]

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
        message: () => `"${result.query}" fails but should pass`,
        pass: false,
      }
    }
  },
})

test.each(testIds)(`${index} ${queryEnv} %s`, async (testId) => {
  const result = await service({ queryEnv, index, testId })
  result.results.forEach((result) => {
    expect(result).toPass()
  })
})
