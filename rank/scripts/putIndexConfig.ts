import { error, info } from './utils'

import { getRankClient } from '../services/elasticsearch'
import { hideBin } from 'yargs/helpers'
import { listIndices } from '../services/search-templates'
import { parse } from 'path'
import prompts from 'prompts'
import { readdirSync } from 'fs'
import yargs from 'yargs/yargs'

global.fetch = require('node-fetch')

async function go(args: typeof argv) {
  const client = getRankClient()
  const sourceIndices = await listIndices()

  const configFiles = readdirSync('./data/indices/').filter((fileName) =>
    fileName.includes('.json')
  )
  const from = await prompts({
    type: 'select',
    name: 'value',
    message: 'From which config file?',
    choices: configFiles.map((choice) => ({ title: choice, value: choice })),
  }).then(({ value }) => value)

  const destIndex = parse(from).name
  const indexConfig = await import(`../data/indices/${from}`).then(
    (mod) => mod.default
  )

  info(`creating index ${destIndex}`)
  const { body: putIndexRes } = await client.indices
    .create({
      index: destIndex,
      body: {
        ...indexConfig,
        settings: {
          ...indexConfig.settings,
          index: {
            ...indexConfig.settings.index,
            number_of_shards: 2,
            number_of_replicas: 1,
          },
        },
      },
    })
    .catch((err) => {
      return err
    })

  if (putIndexRes.acknowledged) info(`created index ${destIndex}`)
  if (!putIndexRes.acknowledged) {
    if (putIndexRes.error.type === 'resource_already_exists_exception') {
      info(`${destIndex} already exists, moving on...`)
    } else {
      error(`couldn't create ${destIndex} with error`)
      console.info(putIndexRes.error)
    }
  }

  if (args.reindex) {
    const sourceIndex = await prompts({
      type: 'select',
      name: 'value',
      message: 'From which source index?',
      choices: sourceIndices.map((index) => ({ title: index, value: index })),
    }).then(({ value }) => value)

    if (sourceIndex) {
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
    } else {
      error(
        `reindex failed as we couldn't find a source index for ${destIndex}`
      )
    }
  }
}

const argv = yargs(hideBin(process.argv))
  .options({
    reindex: { type: 'boolean' },
  })
  .parseSync()

go(argv)
