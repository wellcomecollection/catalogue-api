import { error, info } from './utils'

import { getRankClient } from '../services/elasticsearch'
import { listIndices } from '../services/search-templates'
import prompts from 'prompts'

async function go() {
  const sourceIndices = await listIndices()

  const index = await prompts({
    type: 'select',
    name: 'value',
    message: 'Which index do you want to delete?',
    choices: sourceIndices.map((choice) => ({ title: choice, value: choice })),
  }).then(({ value }) => value)

  info(`deleting index ${index}`)
  const rankClient = getRankClient()
  const deleteIndexRes = await rankClient.indices
    .delete({ index })
    .catch((err) => {
      error(err.meta.body)
      return err
    })
  console.info(deleteIndexRes)
}

go()
