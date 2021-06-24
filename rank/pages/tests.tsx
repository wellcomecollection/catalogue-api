import { GetServerSideProps, NextPage } from 'next'
import absoluteUrl from 'next-absolute-url'
import { useState } from 'react'
import tests from '../data/tests'
import { TestResult } from './api/eval'

type TestValidationProps = {
  index: string
  testId: string
  queryIds: string[]
  namespace: string
}

type LightProps = { color: string }
const Light = ({ color }: LightProps) => {
  return <span className={`bg-${color}-400 w-2 h-2 block`}></span>
}

const TestValidation = ({
  index,
  testId,
  queryIds,
  namespace,
}: TestValidationProps) => {
  const [result, setResult] = useState<TestResult>()
  const [candidateResult, setCandidateResult] = useState<TestResult>()
  return (
    <div>
      <div>
        {queryIds[0]}
        <div className="flex flex-wrap text-xs">
          {result &&
            result.results.map((result) => {
              return (
                <div className="flex-auto flex items-center" key={result.query}>
                  <Light color={result.result.pass ? 'green' : 'red'} />
                  {result.query}
                </div>
              )
            })}
        </div>
      </div>
      <div>
        {queryIds[1]}
        <div className="flex flex-wrap text-xs">
          {candidateResult &&
            candidateResult.results.map((result) => {
              return (
                <div className="flex-auto flex items-center" key={result.query}>
                  <Light color={result.result.pass ? 'green' : 'red'} />
                  <span>{result.query}</span>
                </div>
              )
            })}
        </div>
      </div>
      <button
        type="button"
        onClick={async () => {
          const { origin } = absoluteUrl()
          const resultQs = new URLSearchParams({
            index,
            testId,
            queryId: queryIds[0],
            namespace,
          })
          const resultData: TestResult = await fetch(
            `${origin}/api/test?${resultQs.toString()}`
          ).then((res) => res.json())

          setResult(resultData)

          const candidateResultQs = new URLSearchParams({
            index,
            testId,
            queryId: queryIds[1],
            namespace,
          })
          const candidateResultData: TestResult = await fetch(
            `${origin}/api/test?${candidateResultQs.toString()}`
          ).then((res) => res.json())

          setCandidateResult(candidateResultData)
        }}
      >
        {!result && '🐕'}
      </button>
    </div>
  )
}

const Tests: NextPage = () => {
  return (
    <>
      <h1 className="text-4xl font-bold">Tests</h1>

      <div>
        <h2 className="text-xl font-bold">Works</h2>
        <ul>
          {tests.works.map(({ label, id }) => (
            <li key={`works-${id}`}>
              {label}
              <TestValidation
                {...{
                  index: 'works-2021-06-08',
                  testId: id,
                  queryIds: ['works', 'worksCandidate'],
                  namespace: 'works',
                }}
              />
            </li>
          ))}
        </ul>
      </div>
      <div>
        <h2 className="text-xl font-bold">Images</h2>
        <ul>
          {tests.images.map(({ label, id }) => (
            <li key={`works-${id}`}>{label}</li>
          ))}
        </ul>
      </div>
    </>
  )
}

export const getServerSideProps: GetServerSideProps = async () => {
  return {
    props: {},
  }
}

export default Tests
