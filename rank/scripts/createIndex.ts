import { error, info, success } from './utils'

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

  const sourceIndex = await prompts({
    type: 'select',
    name: 'value',
    message: 'Which index config are you working from?',
    choices: validIndices.map((choice) => ({ title: choice, value: choice })),
  }).then(({ value }) => value)

  const indexConfig = await import(`../data/indices/${sourceIndex}.json`).then(
    (mod) => mod.default
  )

  const namespace = getNamespaceFromIndexName(sourceIndex)
  info(`Your new index should have a name like ${namespace}-with-secret-sauce`)
  const destIndex = await prompts({
    type: 'text',
    name: 'value',
    message: `What should your new index be called?`,
  }).then(({ value }) => value)

  const client = getRankClient()
  info(`Creating index ${destIndex}`)
  const { body: putIndexRes } = await client.indices
    .create({
      index: destIndex,
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
    .catch((err) => {
      return err
    })

  if (putIndexRes.acknowledged) {
    success(`Created index ${destIndex}`)
  } else {
    if (putIndexRes.error.type === 'resource_already_exists_exception') {
      error(`${destIndex} already exists!`)
    } else {
      error(`Couldn't create ${destIndex} with error:`)
      console.info(putIndexRes.error)
    }
  }

  const reindex = await prompts({
    type: 'confirm',
    name: 'value',
    message: `Do you want to start a reindex from ${sourceIndex} into ${destIndex}?`,
    initial: true,
  }).then(({ value }) => value)

  if (reindex) {
    const { body: reindexResp } = await client.reindex({
      wait_for_completion: false,
      body: {
        source: {
          index: sourceIndex,
        },
        dest: {
          index: destIndex,
        },
      },
    })
    console.info(reindexResp)
    info(`reindex started`)
  }
}

go()
