import fs from 'fs'
import prompts from 'prompts'
import yargs from 'yargs/yargs'
import { hideBin } from 'yargs/helpers'
import { error, info, p, pretty } from './utils'
import { getNamespaceFromIndexName, namespaces } from '../types/namespace'
import { getRankClient } from '../services/elasticsearch'
import {
  getRemoteTemplates,
  SearchTemplate,
} from '../services/search-templates'

global.fetch = require('node-fetch')

async function go(args: typeof argv) {
  const client = getRankClient()
  const from =
    args.from ??
    (await prompts({
      type: 'text',
      name: 'value',
      message: 'From which file in /data/indices?',
    }).then(({ value }) => value))

  const indexConfig = await import(`../data/indices/${from}.json`)
    .then((mod) => mod.default)
    .catch(() =>
      error(`index config file "/data/indices/${from}.json" does not exist.`)
    )

  const hasNamespace = Boolean(getNamespaceFromIndexName(from))
  const namespace = hasNamespace
    ? getNamespaceFromIndexName(from)
    : await prompts({
        type: 'select',
        name: 'value',
        message: 'which namespace are you creating this in?',
        choices: namespaces.map((namespace) => ({
          title: namespace,
          value: namespace,
        })),
      }).then(({ value }) => value)

  const index = hasNamespace ? from : `${namespace}-${from}`

  info(`creating index ${from}`)
  const { body: putIndexRes } = await client.indices
    .create({
      index,
      body: {
        ...indexConfig,
        settings: {
          ...indexConfig.settings,
          index: {
            ...indexConfig.settings.index,
            number_of_shards: 1,
            number_of_replicas: 0,
          },
        },
      },
    })
    .catch((err) => {
      return err
    })

  if (putIndexRes.acknowledged) info(`created index ${index}`)
  if (!putIndexRes.acknowledged) {
    if (putIndexRes.error.type === 'resource_already_exists_exception') {
      info(`${index} already exists, moving on...`)
    } else {
      error(`couldn't create ${index} with error`)
      console.info(putIndexRes.error)
    }
  }

  info(`looking for search template /data/search-templates/${index}.json`)
  const searchTemplateExists = fs.existsSync(
    p([`/data/search-templates/${index}.json`])
  )

  if (!searchTemplateExists) {
    const searchTemplate = pretty({
      id: index,
      index,
      namespace,
      env: 'local',
      source: { query: {} },
    } as SearchTemplate)

    info(`writing search templates to /data/search-templates/${index}.json`)
    fs.writeFileSync(
      p([`../data/search-templates/${index}.json`]),
      searchTemplate
    )
  }

  const reindex =
    args.reindex === undefined
      ? await prompts({
          type: 'text',
          name: 'value',
          message: `Reindex into ${index}`,
        }).then(({ value }) => value)
      : args.reindex

  if (reindex) {
    const templates = await getRemoteTemplates('prod')
    const sourceIndex = templates.find(
      (template) => getNamespaceFromIndexName(template.index) === namespace
    )?.index

    if (sourceIndex) {
      const { body: reindexResp } = await client.reindex({
        wait_for_completion: false,
        body: {
          source: {
            index: sourceIndex,
          },
          dest: {
            index,
          },
        },
      })
      console.info(reindexResp)
      info(`reindex started`)
    } else {
      error(
        `reindex failed as we couldn't find a source index for ${index} with namespace ${namespace}`
      )
    }
  }
}

const argv = yargs(hideBin(process.argv))
  .options({
    from: { type: 'string' },
    reindex: { type: 'boolean' },
  })
  .parseSync()

go(argv)
