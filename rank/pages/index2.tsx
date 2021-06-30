import { FC, useEffect, useState } from 'react'
import { GetServerSideProps, NextPage } from 'next'
import { SearchTemplate, getTemplates } from '../services/search-templates'
import { Test, TestResult } from '../types/test'
import absoluteUrl from 'next-absolute-url'
import tests from '../data/tests'
import { removeEmpty } from '../utils'

type Props = {
  searchTemplates: SearchTemplate[]
  worksIndex?: string
  imagesIndex?: string
}

export const getServerSideProps: GetServerSideProps<Props> = async ({
  query,
}) => {
  const { worksIndex, imagesIndex } = query
  const w = worksIndex ? worksIndex.toString() : undefined
  const i = imagesIndex ? imagesIndex.toString() : undefined

  const searchTemplates = await getTemplates({
    worksIndex: w,
    imagesIndex: i,
  })

  const props = removeEmpty({
    searchTemplates,
    worksIndex: w,
    imagesIndex: i,
  })

  return {
    props,
  }
}

type TestRunProps = {
  test: Test
  template: SearchTemplate
  worksIndex?: string
  imagesIndex?: string
}
const TestRun: FC<TestRunProps> = ({
  test,
  template,
  worksIndex,
  imagesIndex,
}) => {
  const [testResult, setTestResult] = useState<TestResult>()
  useEffect(() => {
    const fetchData = async () => {
      const { origin } = absoluteUrl()
      const params = new URLSearchParams(
        removeEmpty({
          testId: test.id,
          templateId: template.id,
          worksIndex,
          imagesIndex,
        })
      )

      const data: TestResult = await fetch(
        `${origin}/api/test?${params.toString()}`
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

export const Index2: NextPage<Props> = ({
  searchTemplates,
  worksIndex,
  imagesIndex,
}) => {
  const worksTemplates = searchTemplates.filter((t) => t.namespace === 'works')
  const worksTests = tests.works
  // const imagesTemplates = searchTemplates.filter(
  //   (t) => t.namespace === 'images'
  // )
  // const imagesTests = tests.images

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
                  {...{ test, template, worksIndex, imagesIndex }}
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
