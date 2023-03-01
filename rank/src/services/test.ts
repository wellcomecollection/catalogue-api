import { Cluster, Index, QueryEnv } from '../types/searchTemplate'
import { TestCase, TestResult } from '../types/test'
import { getPipelineClient, getRankClient } from './elasticsearch'

import { Client } from '@elastic/elasticsearch'
import { getTemplate } from './search-templates'
import tests from '../tests'

type Props = {
  testId: string
  queryEnv: QueryEnv
  index: Index
  cluster: Cluster
}

export const indexExists = async (
  client: Client,
  index: string
): Promise<boolean> => {
  try {
    await client.indices.get({ index })
    return true
  } catch (e) {
    return false
  }
}

export async function getClient(queryEnv, cluster, template): Promise<Client> {
  if (cluster === 'pipeline' && queryEnv === 'candidate') {
    const prodClient = await getPipelineClient('production')
    const stageClient = await getPipelineClient('staging')
    if (await indexExists(prodClient, template.index)) {
      return prodClient
    } else if (await indexExists(stageClient, template.index)) {
      return stageClient
    } else {
      throw new Error(
        `${template.index} does not exist in any currently used ${cluster} cluster!`
      )
    }
  } else if (cluster === 'pipeline') {
    return await getPipelineClient(queryEnv)
  } else if (cluster === 'rank') {
    return await getRankClient()
  } else {
    throw new Error(`Unknown cluster ${cluster}`)
  }
}

async function rankTest({
  queryEnv,
  cluster,
  index,
  testId
}: Props): Promise<TestResult> {
  const template = await getTemplate(queryEnv, index)

  const test = tests[template.namespace].find((test) => test.id === testId)
  if (!test) throw Error(`No such test ${testId}`)

  const { cases, metric } = test

  const requests = cases.map((testCase: TestCase) => {
    return {
      id: testCase.searchTerms,
      template_id: template.id,
      params: {
        query: testCase.searchTerms
      },
      ratings: testCase.ratings.map((id) => {
        return {
          _id: id,
          _index: template.index,
          rating: 3
        }
      })
    }
  })

  const client = await getClient(queryEnv, cluster, template)
  // fail if the index doesn't exist
  if (!(await indexExists(client, template.index))) {
    throw new Error(`${index} does not exist in the ${cluster} cluster!`)
  }

  const { details } = await client.rankEval({
    index: template.index,
    body: {
      requests,
      metric,
      // The types do not support this use case :(
      // @ts-ignore
      templates: [
        {
          id: template.id,
          template: { source: { query: template.query } }
        }
      ]
    }
  })

  const results = Object.entries(details).map(([query, detail]) => {
    const testCase = test.cases.find((c) => c.searchTerms === query)
    return {
      searchTerms: query,
      description: testCase?.description,
      knownFailure: testCase?.knownFailure,
      result: test.eval(detail)
    }
  })

  return {
    index: template.index,
    queryEnv: template.queryEnv,
    description: test.description,
    namespace: template.namespace,
    pass: results.every((result) => result.result.pass),
    results
  }
}

export default rankTest
export type { Props }
