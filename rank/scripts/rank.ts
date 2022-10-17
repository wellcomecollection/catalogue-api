import { Index, QueryEnv, queryEnvs } from '../types/searchTemplate'
import { code, gatherArgs, info } from './utils'
import { getPipelineClient, getRankClient } from '../services/elasticsearch'

import { Client } from '@elastic/elasticsearch'
import { exec } from 'child_process'
import { listIndices } from '../services/search-templates'
import { tests as possibleTests } from '../tests'

global.fetch = require('node-fetch')

async function go() {
  const { cluster, query } = await gatherArgs({
    cluster: { type: 'string', choices: ['pipeline', 'rank'] },
    query: { type: 'string', choices: queryEnvs },
  })

  const queryEnv = query as QueryEnv
  let client: Client
  if (cluster === 'pipeline') {
    client = await getPipelineClient(queryEnv)
  } else if (cluster === 'rank') {
    client = await getRankClient()
  } else {
    throw new Error(`Unknown cluster ${cluster}`)
  }

  const indices: Index[] = await listIndices(client)
  const possibleTestIds = Object.values(possibleTests)
    .flatMap((testSet) => testSet.map((test) => test.id))
    // Some test IDs are common across suites - we don't want to display
    // duplicates, so we filter here to keep only the unique values
    .filter((v, i, a) => a.indexOf(v) == i)

  const args = await gatherArgs({
    index: { type: 'string', choices: indices },
    testId: {
      type: 'array',
      choices: possibleTestIds,
      default: possibleTestIds,
    },
  })

  const index = args.index as Index
  const testIds = args.testId as string[]

  // run the rank tests using the collected arguments
  const command =
    `yarn test --index=${index} --queryEnv=${queryEnv} --cluster=${cluster} ` +
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
