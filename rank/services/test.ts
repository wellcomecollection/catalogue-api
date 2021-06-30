import { TestCase, TestResult } from '../types/test'
import { decodeString } from './decoder'
import { Decoder } from '../types/decoder'
import { ParsedUrlQuery } from 'querystring'
import { RankEvalResponse } from '../types/elasticsearch'
import { getRankClient } from './elasticsearch'
import getTemplates, { Props as SearchTemplateProps } from './search-templates'
import tests from '../data/tests'

type Props = {
  testId: string
  templateId: string
} & SearchTemplateProps

/**
 * This service takes fetches a test from a `namespace`
 * looks up the query from our local or remote search-templates
 * and generates results from a rank_eval requst
 */
async function service({
  templateId,
  testId,
  ...templateProps
}: Props): Promise<TestResult> {
  const templates = await getTemplates(templateProps)
  const template = templates.find((template) => template.id === templateId)
  const test = tests[template.namespace].find((test) => test.id === testId)

  if (!test) throw Error(`No such test ${testId}`)

  const { cases, metric, searchTemplateAugmentation } = test

  const searchTemplate = searchTemplateAugmentation
    ? searchTemplateAugmentation(test, { ...template.source })
    : { ...template.source }

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

  const reqBody = {
    requests,
    metric,
    templates: [{ id: template.id, template: { inline: searchTemplate } }],
  }

  const { body: rankEvalRes } =
    await getRankClient().rankEval<RankEvalResponse>({
      index: template.index,
      body: reqBody,
    })

  const results = Object.entries(rankEvalRes.details).map(([query, detail]) => {
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
    env: template.env,
    label: test.label,
    description: test.description,
    namespace: template.namespace,
    pass: results.every((result) => result.result.pass),
    results,
  }
}

export const decoder: Decoder<Props> = (q: ParsedUrlQuery) => ({
  testId: decodeString(q, 'testId'),
  templateId: decodeString(q, 'templateId'),
  worksIndex: q.worksIndex ? decodeString(q, 'worksIndex') : undefined,
  imagesIndex: q.imagesIndex ? decodeString(q, 'imagesIndex') : undefined,
})

export default service
export type { Props }
