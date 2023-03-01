import { Index, QueryEnv, queryEnvs } from '../src/types/searchTemplate'

import chalk from 'chalk'
import fetch from 'node-fetch'
import { gatherArgs } from './utils'
import { getRankClient } from '../src/services/elasticsearch'
import { listIndices } from '../src/services/search-templates'
import prompts from 'prompts'
import search from '../src/services/search'

async function go() {
  const client = await getRankClient()
  const indices: Index[] = await listIndices(client)

  const args = await gatherArgs({
    index: { type: 'string', choices: indices },
    queryEnv: { type: 'string', choices: queryEnvs }
  })

  const index = args.index as Index
  const queryEnv = args.queryEnv as QueryEnv

  const searchTerms = await prompts({
    type: 'text',
    name: 'value',
    message: 'What are you looking for?'
  }).then(({ value }) => value)

  const results = await search({ index, queryEnv, searchTerms })

  results.hits.hits.map((hit, i) => {
    const title = hit._source.display.title
    const url = `https://wellcomecollection.org/works/${hit._id}`
    const reference = hit._source.display.referenceNumber
    const dates = hit._source.display.production
      .flatMap((p) => p.dates.map((d) => d.label))
      .join(', ')
    const displayString = [
      `${i + 1}.\t${chalk.bold(chalk.cyan(title))}`,
      reference ? reference : null,
      dates ? dates : null,
      url
    ]
      .filter((n) => n)
      .join('\n\t')
    console.log(displayString + '\n')
  })
}

go()
