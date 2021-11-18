import { Index, QueryEnv, queryEnvs } from '../types/searchTemplate'
import { code, gatherArgs, info } from './utils'

import { exec } from 'child_process'
import { listIndices } from '../services/search-templates'
import { tests as possibleTests } from '../data/tests'

global.fetch = require('node-fetch')

async function go() {
  const indices: Index[] = await listIndices()

  const possibleTestIds = Object.values(possibleTests)
    .flatMap((testSet) => testSet.map((test) => test.id))
    .filter((v, i, a) => a.indexOf(v) == i) // keep unique

  const args = await gatherArgs({
    index: { type: 'string', choices: indices },
    query: { type: 'string', choices: queryEnvs },
    testId: {
      type: 'array',
      choices: possibleTestIds,
      default: possibleTestIds,
    },
  })

  const index = args.index as Index
  const queryEnv = args.query as QueryEnv
  const testIds = args.testId as string[]

  // run the rank tests using the collected arguments
  const command =
    `yarn test --index=${index} --queryEnv=${queryEnv} ` +
    testIds.map((id) => `--testId=${id}`).join(' ')

  info('Running:')
  code(command)

  exec(command, (error, stdout, stderr) => {
    if (error) {
      console.error(error)
      return
    }
    console.log(stdout)
    console.error(stderr)
  })
}

go()
