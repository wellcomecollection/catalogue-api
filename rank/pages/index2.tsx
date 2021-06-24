import { GetServerSideProps, NextPage } from 'next'
import absoluteUrl from 'next-absolute-url'
import { FC, useEffect, useState } from 'react'
import tests from '../data/tests'
import service, { SearchTemplate } from '../services/search-templates'
import { Env } from '../types/env'
import { Namespace } from '../types/namespace'
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
  index: string
  env: Env
  testId: string
  namespace: Namespace
}
const TestRun: FC<TestRunProps> = ({ index, env, testId, namespace }) => {
  const [testResult, setTestResult] = useState<TestResult>()

  const envIndex = `${env}:${index}`
  useEffect(() => {
    const fetchData = async () => {
      const { origin } = absoluteUrl()
      const data: TestResult = await fetch(
        `${origin}/api/test?index=${envIndex}&testId=${testId}&namespace=${namespace}`
      ).then((res) => res.json())

      setTestResult(data)
    }
    fetchData()
  }, [])

  return (
    <div>
      {testResult && (
        <>
          {testResult && (
            <div>
              <h2 className="text-lg">{env}</h2>
              {testResult.results.map((result, i) => (
                <div key={`${testId}-${env}-${i}`}>
                  <pre className="inline-block">
                    {result.result.pass ? ' pass ' : ' fail '}
                  </pre>
                  | {result.query}
                </div>
              ))}
            </div>
          )}
        </>
      )}
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
      {worksTests.map(({ id: testId, label, description, cases }) => (
        <div key={`works-${testId}`}>
          <h2 className="text-xl">{label}</h2>
          <div className={`grid grid-cols-${worksTemplates.length}`}>
            {worksTemplates.map(({ env, namespace, index }) => {
              return (
                <TestRun
                  key={`works-${testId}-${env}-${index}`}
                  {...{ testId, index, env, namespace }}
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
