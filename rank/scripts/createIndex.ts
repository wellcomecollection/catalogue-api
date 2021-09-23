import { code, error, info, success } from './utils'

import { getNamespaceFromIndexName } from '../types/searchTemplate'
import { getRankClient } from '../services/elasticsearch'
import { listIndices } from '../services/search-templates'
import { parse } from 'path'
import prompts from 'prompts'
import { readdirSync } from 'fs'

global.fetch = require('node-fetch')

async function go() {
  // Users should be able to create new indices by modifying mappings they've
  // fetched from the rank cluster, where those source indices still exist.
  const configFiles = readdirSync('./data/indices/')
    .filter((fileName) => fileName.includes('.json'))
    .map((fileName) => parse(fileName).name)
  const sourceIndices = await listIndices()
  const validIndices = sourceIndices.filter((value) =>
    configFiles.includes(value)
  )

  const localIndex = await prompts({
    type: 'select',
    name: 'value',
    message: 'Which index config are you working from?',
    choices: validIndices.map((index) => ({ title: index, value: index })),
  }).then(({ value }) => value)

  const indexConfig = await import(`../data/indices/${localIndex}.json`).then(
    (mod) => mod.default
  )

  const namespace = getNamespaceFromIndexName(localIndex)
  info(`Your new index should start with it's namespace "works-" or "images-"`)
  const remoteIndex = await prompts({
    type: 'text',
    name: 'value',
    message: `What should your new index be called?`,
  }).then(({ value }) => value)

  const client = getRankClient()
  const { body: putIndexRes } = await client.indices.create({
    index: remoteIndex,
    body: {
      ...indexConfig,
      settings: {
        ...indexConfig.settings,
        index: {
          ...indexConfig.settings.index,
          number_of_shards: 1,
          number_of_replicas: 1,
        },
      },
    },
  })
  if (putIndexRes.acknowledged) {
    success(`Created index ${remoteIndex}`)
  } else {
    if (putIndexRes.error.type === 'resource_already_exists_exception') {
      error(`${remoteIndex} already exists!`)
    } else {
      error(`Couldn't create ${remoteIndex} with error:`)
      console.info(putIndexRes.error)
    }
  }

  const reindex = await prompts({
    type: 'confirm',
    name: 'value',
    message: `Do you want to start a reindex from ${localIndex} into ${remoteIndex}?`,
    initial: true,
  }).then(({ value }) => value)

  if (reindex) {
    const { body: reindexResp } = await client.reindex({
      wait_for_completion: false,
      body: {
        source: {
          index: localIndex,
        },
        dest: {
          index: remoteIndex,
        },
      },
    })
    if (reindexResp.acknowledged) {
      success('Reindex started successfully')
      info('You can monitor the reindex task by running:')
      code(`  yarn checkTask ${reindexResp.task}`)
    } else {
      error('Failed to start reindex with error:')
      console.info(reindexResp.error)
    }
  }
}

go()
