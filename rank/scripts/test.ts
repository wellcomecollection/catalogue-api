import { Cluster, Index, QueryEnv } from '../src/types/searchTemplate'

import { TestResult } from '../src/types/test'
import service from '../src/services/test'
import tests from '../src/tests'
import yargs from 'yargs'

global.fetch = require('node-fetch')

const args = yargs(process.argv)
  .options({
    queryEnv: { type: 'string', demandOption: true },
    cluster: { type: 'string', demandOption: true },
    index: { type: 'string', demandOption: true },
    testId: { type: 'array', demandOption: true }
  })
  .exitProcess(false)
  .parseSync()

const queryEnv = args.queryEnv as QueryEnv
const cluster = args.cluster as Cluster
const index = args.index as Index
const testIds = args.testId as string[]



// for each testId, run the test and print the results
testIds.forEach(async (testId) => {
  const result = await service({ queryEnv, index, testId, cluster })
  result.results.forEach((result) => {
    if (result.knownFailure) {
      return {
        message: () => `"${result.query}" is a known failure`,
        pass: true
      }
    }
    if (result.result.pass) {
      return {
        message: () => `"${result.query}" passes`,
        pass: true
      }
    } else {
      return {
        message: () => `"${result.query}" fails but should pass`,
        pass: false
      }
    }
  })

  console.log(result)
})
