import { code, error, info, success } from './utils'

import fetch from 'node-fetch'
import { getNamespaceFromIndexName } from '../src/types/searchTemplate'
import { getRankClient } from '../src/services/elasticsearch'
import { listIndices } from '../src/services/search-templates'
import { parse } from 'path'
import prompts from 'prompts'
import { readdirSync } from 'fs'

async function go() {
  // Users should be able to create new indices by modifying mappings they've
  // fetched from the rank cluster, where those source indices still exist.
  const configFiles = readdirSync('./data/mappings/')
    .filter((fileName) => fileName.includes('.json'))
    .map((fileName) => parse(fileName).name)
  const client = await getRankClient()
  const sourceIndices = await listIndices(client)
  const validIndices = sourceIndices.filter((value) =>
    configFiles.includes(value)
  )

  const localIndex = await prompts({
    type: 'select',
    name: 'value',
    message: 'Which index config are you working from?',
    choices: validIndices.map((index) => ({ title: index, value: index }))
  }).then(({ value }) => value)

  const indexConfig = await import(`../data/mappings/${localIndex}.json`).then(
    (mod) => mod.default
  )

  const remoteIndex = await prompts({
    type: 'text',
    name: 'value',
    message: 'What should the new index be called?',
    initial: `${getNamespaceFromIndexName(localIndex)}-candidate`
  }).then(({ value }) => value)

  const putIndexRes = await client.indices.create({
    index: remoteIndex,
    body: {
      ...indexConfig,
      settings: {
        ...indexConfig.settings,
        index: {
          ...indexConfig.settings.index,
          number_of_shards: 1,
          number_of_replicas: 1
        }
      }
    }
  })
  if (putIndexRes.acknowledged) {
    success(`Created index ${remoteIndex}`)
  } else {
    // Don't think the client has the right types for this
    // @ts-ignore
    if (putIndexRes.error.type === 'resource_already_exists_exception') {
      error(`${remoteIndex} already exists!`)
    } else {
      error(`Couldn't create ${remoteIndex} with error:`)
      // @ts-ignore
      console.info(putIndexRes.error)
    }
  }

  const reindex = await prompts({
    type: 'confirm',
    name: 'value',
    message: `Do you want to start a reindex from ${localIndex} into ${remoteIndex}?`,
    initial: true
  }).then(({ value }) => value)

  if (reindex) {
    const { nDocsRequested } = await prompts({
      type: 'number',
      name: 'nDocsRequested',
      message:
        'How many documents do you want to reindex? (0 for all documents)',
      initial: 0
    })
    const reindexResp = await client.reindex({
      wait_for_completion: false,
      max_docs: nDocsRequested === 0 ? undefined : nDocsRequested,
      body: {
        source: {
          index: localIndex,
          size: 100 // batch size reduced from 1000 to avoid memory issues during reindex
        },
        dest: {
          index: remoteIndex
        }
      }
    })

    success('Reindex started successfully')
    info('You can monitor the reindex by running:')
    code(`yarn checkTask --task ${reindexResp.task}`)
  }
}

go()
