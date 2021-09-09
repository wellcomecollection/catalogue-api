import { FC, useEffect, useState } from 'react'
import { H2, H3 } from './H'
import Result, { resultColor } from './Result'
import { SearchTemplate, namespaces } from '../types/searchTemplate'
import { Test, TestResult } from '../types/test'
import { groupBy, removeEmpty } from '../services/utils'

import { Meta } from './Text'
import absoluteUrl from 'next-absolute-url'
import tests from '../data/tests'

type TestResultsProps = {
  test: Test
  templates: SearchTemplate[]
}
const TestResults: FC<TestResultsProps> = ({ templates, test }) => {
  const [results, setResults] = useState<TestResult[]>([])
  useEffect(() => {
    const fetchData = async () => {
      const { origin } = absoluteUrl()
      const resultsReq = templates.map(async (template) => {
        const params = new URLSearchParams(
          removeEmpty({
            env: template.env,
            index: template.index,
            testId: test.id,
          })
        )
        const data: TestResult = await fetch(
          `${origin}/api/test?${params.toString()}`
        ).then((res) => res.json())
        return data
      })
      const results = await Promise.all(resultsReq)
      setResults(results)
    }
    fetchData()
  })

  const groupedResults = groupBy(
    // concatenate only the results
    results.reduce(
      (acc, result) => acc.concat(result.results),
      [] as TestResult['results']
    ),
    // group by the query
    (i) => i.query
  )

  return (
    <table className="table-auto w-full max-w-lg bg-pink-50">
      <thead className="sr-only">
        <tr>
          {['query', results.map(({ env }) => env)].map((label) => {
            return (
              <th key={`${label}`} className="text-left font-normal">
                <Meta>{label}</Meta>
              </th>
            )
          })}
        </tr>
      </thead>
      <tbody>
        {results &&
          Object.entries(groupedResults).map(([query, result]) => {
            return (
              <tr key={`${query}`}>
                <td className="border border-pink-200 px-4 py-2">{query}</td>
                {result.map((result) => (
                  <td
                    // This key is definitely wrong, annoyingly I can't find out how to construct something unique here.
                    key={`${Math.random().toString(36).substring(7)}`}
                    className={`w-6 border border-pink-200 bg-${resultColor(
                      result.result.pass
                    )}`}
                  >
                    <Result pass={result.result.pass} />
                  </td>
                ))}
              </tr>
            )
          })}
      </tbody>
    </table>
  )
}

type Props = {
  searchTemplates: SearchTemplate[]
}
const SearchTemplateTests: FC<Props> = ({ searchTemplates }) => {
  return (
    <>
      {namespaces.map((namespace) => {
        const namespacedTemplates = searchTemplates.filter(
          (template) => template.namespace === namespace
        )
        return (
          <div key={namespace}>
            <div>
              <H2>{namespace}</H2>
              {tests[namespace].map((test) => {
                return (
                  <div key={`${namespace}-${test.id}`} className="mb-8">
                    <H3>{test.label}</H3>
                    <TestResults test={test} templates={namespacedTemplates} />
                  </div>
                )
              })}
            </div>
          </div>
        )
      })}
    </>
  )
}

export default SearchTemplateTests
