import { code, error, info, success } from './utils'

import { getNamespaceFromIndexName } from '../types/searchTemplate'
import { getRankClient } from '../services/elasticsearch'
import { listIndices } from '../services/search-templates'
import { parse } from 'path'
import prompts from 'prompts'
import { readdirSync } from 'fs'

async function go() {
  // Users should be able to update indices by modifying mappings they've
  // fetched from the rank cluster, where those source indices still exist.
  const client = await getRankClient()
  const sourceIndices = await listIndices(client)

  const indexName = await prompts({
    type: 'select',
    name: 'value',
    message: 'Which index do you want to update?',
    choices: sourceIndices.map((choice) => ({ title: choice, value: choice })),
  }).then(({ value }) => value)

  // the config should have the same namespace as the chosen index
  const indexNamespace = getNamespaceFromIndexName(indexName)
  const configFiles = readdirSync('./mappings/')
    .filter((fileName) => fileName.includes('.json'))
    .map((fileName) => parse(fileName).name)
    .filter((index) => indexNamespace == getNamespaceFromIndexName(index))

  const configName = await prompts({
    type: 'select',
    name: 'value',
    message: `Which config do you want to apply to ${indexName}?`,
    choices: configFiles.map((choice) => ({ title: choice, value: choice })),
  }).then(({ value }) => value)

  const indexConfig = await import(`../mappings/${configName}.json`).then(
    (mod) => mod.default
  )

  const rankClient = await getRankClient()

  const closeIndexRes = await rankClient.indices.close({
    index: indexName,
  })
  if (closeIndexRes.acknowledged) {
    success(`Closed ${indexName}`)
  } else {
    error(`Couldn't close ${indexName} with error:`)
    console.info(closeIndexRes)
  }

  const putSettingsRes = await rankClient.indices.putSettings({
    index: indexName,
    body: {
      ...indexConfig.settings,
    },
  })
  if (putSettingsRes.acknowledged) {
    success(`Posted settings to ${indexName}`)
  } else {
    error(`Couldn't post settings to ${indexName} with error:`)
    console.info(putSettingsRes)
  }

  const putMappingRes = await rankClient.indices.putMapping({
    index: indexName,
    body: {
      ...indexConfig.mappings,
    },
  })
  if (putMappingRes.acknowledged) {
    success(`Posted mappings to ${indexName}`)
  } else {
    error(`Couldn't post mappings to ${indexName} with error:`)
    console.info(putMappingRes)
  }

  const openIndexRes = await rankClient.indices.open({
    index: indexName,
  })
  if (openIndexRes.acknowledged) {
    success(`Reopened ${indexName}`)
  } else {
    error(`Couldn't reopen ${indexName} with error:`)
    console.info(openIndexRes)
  }

  const updateByQuery = await prompts({
    type: 'confirm',
    name: 'value',
    message: 'Do you want to run the updateByQuery task now?',
    initial: true,
  }).then(({ value }) => value)

  if (updateByQuery) {
    const updateByQueryRes = await rankClient.updateByQuery({
      wait_for_completion: false,
      index: indexName,
      conflicts: 'proceed',
    })

    const taskID = updateByQueryRes.task
    success(`Update started successfully with task ID ${taskID}`)
    info('You can monitor the task by running:')
    code(`yarn checkTask --task ${taskID}`)
  }
}

go()
