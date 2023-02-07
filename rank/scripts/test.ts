import { Cluster, Index, QueryEnv } from '../src/types/searchTemplate'

import rankTest from '../src/services/test'
import testOrder from '../src/services/order'
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

Promise.all(
  testIds.map(async (testId) => {
    const result =
      testId === 'order'
        ? await testOrder({ queryEnv, index, testId, cluster })
        : await rankTest({ queryEnv, index, testId, cluster })

    return result.results.map((result) => {
      if (result.knownFailure) {
        return {
          message: `🟡\t"${result.searchTerms}" is a known failure\n\t${result.description}\n`,
          pass: true
        }
      }
      if (result.result.pass) {
        return {
          message: `🟢\t"${result.searchTerms}" passes\n\t${result.description}\n`,
          pass: true
        }
      } else {
        return {
          message: `🔴\t"${result.searchTerms}" fails but should pass\n\t${result.description}\n`,
          pass: false
        }
      }
    })
  })
).then((results) => {
  results.flat().forEach((result) => {
    console.log(result.message)
  }, console.error)
  const allPass = results
    .flat()
    .map((result) => result.pass)
    .reduce((acc, curr) => acc && curr, true)
  if (!allPass) {
    process.exit(1)
  }
}, console.error)
