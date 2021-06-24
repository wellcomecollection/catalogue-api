import { GetServerSideProps, NextPage } from 'next'
import absoluteUrl from 'next-absolute-url'
import { FC, useEffect, useState } from 'react'
import tests from '../data/tests'
import service, { SearchTemplate } from '../services/search-templates'
import { Env } from '../types/env'
import { Namespace } from '../types/namespace'
import { Test } from '../types/test'
import { TestResult } from './api/eval'

type Props = {
  searchTemplates: SearchTemplate[]
}

export const getServerSideProps: GetServerSideProps<Props> = async () => {
  const searchTemplates = await service()
  return {
    props: {
      searchTemplates,
    },
  }
}

type TestRunProps = {
  test: Test
  template: SearchTemplate
}
const TestRun: FC<TestRunProps> = ({ test, template }) => {
  const [testResult, setTestResult] = useState<TestResult>()

  const envIndex = `${template.env}:${template.index}`
  useEffect(() => {
    const fetchData = async () => {
      const { origin } = absoluteUrl()
      const data: TestResult = await fetch(
        `${origin}/api/test?index=${envIndex}&testId=${test.id}&namespace=${template.namespace}`
      ).then((res) => res.json())

      setTestResult(data)
    }
    fetchData()
  }, [])

  return (
    <div>
      <>
        <div>
          <div className="text-sm">
            <div>env: {template.env}</div>
            <div>index: {template.index}</div>
          </div>
          {testResult &&
            testResult.results.map((result, i) => (
              <div key={`${test.id}-${template.env}-${i}`}>
                <pre className="inline-block">
                  {result.result.pass ? ' pass ' : ' fail '}
                </pre>
                | {result.query}
              </div>
            ))}
        </div>
      </>
    </div>
  )
}

export const Index2: NextPage<Props> = ({ searchTemplates }) => {
  const worksTemplates = searchTemplates.filter((t) => t.namespace === 'works')
  const worksTests = tests.works
  const imagesTemplates = searchTemplates.filter(
    (t) => t.namespace === 'images'
  )
  const imagesTests = tests.images

  return (
    <>
      {worksTests.map((test) => (
        <div key={`works-${test.id}`}>
          <h2 className="text-xl">{test.label}</h2>
          <div className={`grid grid-cols-${worksTemplates.length}`}>
            {worksTemplates.map((template) => {
              return (
                <TestRun
                  key={`works-${test.id}-${template.env}-${template.index}`}
                  {...{ test, template }}
                />
              )
            })}
          </div>
        </div>
      ))}
    </>
  )
}

export default Index2
