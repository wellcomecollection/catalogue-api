import { FC, useEffect, useState } from 'react'
import { GetServerSideProps, NextPage } from 'next'
import absoluteUrl from 'next-absolute-url'
import {
  SearchTemplate,
  getTemplates,
  listIndices,
} from '../services/search-templates'
import { Test, TestResult } from '../types/test'
import tests from '../data/tests'
import { removeEmpty } from '../utils'
import Form, { Label, FormBar, Select, Submit } from '../components/Form'
import Result from '../components/Result'
import { H2 } from '../components/H'
import { Meta } from '../components/Text'
import { string } from 'yargs'

type Props = {
  searchTemplates: SearchTemplate[]
  worksIndex?: string
  imagesIndex?: string
  indices: string[]
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
  const indices = await listIndices()

  return {
    props: removeEmpty({
      searchTemplates,
      worksIndex: w,
      imagesIndex: i,
      indices,
    }),
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
          <Meta>
            <div>env: {template.env}</div>
            <div>index: {template.index}</div>
          </Meta>
          {testResult &&
            testResult.results.map((result, i) => (
              <div key={`${test.id}-${template.env}-${i}`}>
                <pre className="inline-block mr-2">
                  <Result pass={result.result.pass} />
                </pre>
                <a
                  href={`/search?query=${result.query}&index=${template.index}`}
                >
                  {result.query}
                </a>
              </div>
            ))}
        </div>
      </>
    </div>
  )
}

type IndexSelectorProps = {
  worksIndex?: string
  imagesIndex?: string
  indices: string[]
}
const IndexSelector: FC<IndexSelectorProps> = ({
  worksIndex,
  imagesIndex,
  indices,
}) => {
  const [worksIndexVal, setWorksIndexVal] = useState(worksIndex)
  const [imagesIndexVal, setImagesIndexVal] = useState(imagesIndex)
  return (
    <>
      <Label>
        Works index
        <Select
          name="worksIndex"
          value={worksIndexVal}
          onChange={(event) => setWorksIndexVal(event.currentTarget.value)}
        >
          <option value="">default</option>
          {indices
            .filter((index) => index.startsWith('works'))
            .map((index) => (
              <option key={index}>{index}</option>
            ))}
        </Select>
      </Label>
      <Label>
        Images index
        <Select
          name="imagesIndex"
          value={imagesIndexVal}
          onChange={(event) => setImagesIndexVal(event.currentTarget.value)}
        >
          <option value="">default</option>
          {indices
            .filter((index) => index.startsWith('images'))
            .map((index) => (
              <option key={index}>{index}</option>
            ))}
        </Select>
      </Label>
    </>
  )
}

export const Index2: NextPage<Props> = ({
  searchTemplates,
  worksIndex,
  imagesIndex,
  indices,
}) => {
  const worksTemplates = searchTemplates.filter((t) => t.namespace === 'works')
  const worksTests = tests.works
  // const imagesTemplates = searchTemplates.filter(
  //   (t) => t.namespace === 'images'
  // )
  // const imagesTests = tests.images
  return (
    <>
      <Form>
        <div className={`grid grid-cols-${worksTemplates.length}`}>
          <div>
            <IndexSelector
              worksIndex={worksIndex}
              imagesIndex={imagesIndex}
              indices={[]}
            />
          </div>
          <div className="flex">
            <div>
              <IndexSelector
                worksIndex={worksIndex}
                imagesIndex={imagesIndex}
                indices={indices}
              />
            </div>
            <div className="flex-shrink">
              <Submit />
            </div>
          </div>
        </div>
      </Form>
      {worksTests.map((test) => (
        <div key={`works-${test.id}`} className="mb-5">
          <H2>{test.label}</H2>
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
