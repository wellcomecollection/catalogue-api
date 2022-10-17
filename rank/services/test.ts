import {
  Cluster,
  Index,
  QueryEnv,
  SearchTemplate,
} from '../types/searchTemplate'
import { TestCase, TestResult } from '../types/test'
import { getPipelineClient, getRankClient } from './elasticsearch'

import { Client } from '@elastic/elasticsearch'
import { getTemplate } from './search-templates'
import { tests } from '../tests'

type Props = {
  testId: string
  queryEnv: QueryEnv
  index: Index
  cluster: Cluster
}

const indexExists = async (client: Client, index: string): Promise<boolean> => {
  try {
    await client.indices.get({ index })
    return true
  } catch (e) {
    return false
  }
}

async function service({
  queryEnv,
  cluster,
  index,
  testId,
}: Props): Promise<TestResult> {
  const notAugmentedTemplate = await getTemplate(queryEnv, index)

  const test = tests[notAugmentedTemplate.namespace].find(
    (test) => test.id === testId
  )
  if (!test) throw Error(`No such test ${testId}`)

  const { cases, metric, searchTemplateAugmentation } = test

  const template = searchTemplateAugmentation
    ? new SearchTemplate(
        queryEnv,
        index,
        searchTemplateAugmentation(test, notAugmentedTemplate.query)
      )
    : notAugmentedTemplate

  const requests = cases.map((testCase: TestCase) => {
    return {
      id: testCase.query,
      template_id: template.id,
      params: {
        query: testCase.query,
      },
      ratings: testCase.ratings.map((id) => {
        return {
          _id: id,
          _index: template.index,
          rating: 3,
        }
      }),
    }
  })

  let client: Client
  if (queryEnv === 'candidate' && cluster === 'pipeline') {
    const prodClient = await getPipelineClient('production')
    const stageClient = await getPipelineClient('staging')
    if (await indexExists(prodClient, template.index)) {
      client = prodClient
    } else if (await indexExists(stageClient, template.index)) {
      client = stageClient
    } else {
      throw new Error(
        `${index} does not exist in any currently used ${cluster} cluster!`
      )
    }
  } else if (cluster === 'pipeline') {
    client = await getPipelineClient(queryEnv)
  } else if (cluster === 'rank') {
    client = await getRankClient()
  } else {
    throw new Error(`Unknown cluster ${cluster}`)
  }

  // fail if the index doesn't exist
  if (!(await indexExists(client, template.index))) {
    throw new Error(`${index} does not exist in the ${cluster} cluster!`)
  }

  const res = await client.rankEval({
    index: template.index,
    body: {
      requests,
      metric,
      // The types do not support this use case :(
      // @ts-ignore
      templates: [
        {
          id: template.id,
          template: { source: { query: template.query } },
        },
      ],
    },
  })

  const results = Object.entries(res.details).map(([query, detail]) => {
    const testCase = test.cases.find((c) => c.query === query)
    return {
      query,
      description: testCase?.description,
      knownFailure: testCase?.knownFailure,
      result: test.eval(detail),
    }
  })

  return {
    index: template.index,
    queryEnv: template.queryEnv,
    label: test.label,
    description: test.description,
    namespace: template.namespace,
    pass: results.every((result) => result.result.pass),
    results,
  }
}

export default service
export type { Props }
