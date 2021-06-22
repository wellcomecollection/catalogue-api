import { GetServerSideProps, NextPage } from 'next'
import Link from 'next/link'
import { ParsedUrlQuery } from 'querystring'
import tests from '../data/tests'
import { decoder } from './api/test'
import { Props as ServiceProps } from '../services/test'
import absoluteUrl from 'next-absolute-url'
import { TestResult } from './api/eval'

type Props = {
  search: ServiceProps | undefined
  data: TestResult | undefined
  indices: string[]
}

const Tests: NextPage<Props> = ({ indices, search, data }) => {
  return (
    <>
      <h1 className="text-4xl font-bold">Tests</h1>
      {JSON.stringify(data)}
      <div className="grid grid-cols-2 gap-4">
        <div>
          <h2 className="text-xl font-bold">Works</h2>
          <ul>
            {tests.works.map(({ label, id }) => (
              <li key={`works-${id}`}>
                <Link
                  href={{
                    pathname: '/tests',
                    query: {
                      index: `works-${indices[0]}`,
                      testId: id,
                      queryId: 'works',
                      namespace: 'works',
                    },
                  }}
                >
                  <a>{label}</a>
                </Link>
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
      </div>
    </>
  )
}

type DecoderProps = ReturnType<typeof decoder>
function decode(qs: ParsedUrlQuery): DecoderProps | undefined {
  try {
    return decoder(qs)
  } catch (err) {
    return undefined
  }
}

async function getData(req, search: DecoderProps): Promise<TestResult> {
  const { origin } = absoluteUrl(req)
  const q = new URLSearchParams(search)
  // TODO: we should infer this from the service
  const data: TestResult = await fetch(
    `${origin}/api/test?${q.toString()}`
  ).then((res) => res.json())

  return data
}

export const getServerSideProps: GetServerSideProps<Props> = async ({
  query: qs,
  req,
}) => {
  const search = decode(qs)
  const data = search ? await getData(req, search) : undefined

  return {
    props: JSON.parse(
      JSON.stringify({
        search,
        data,
        indices: ['2021-06-08'],
      })
    ),
  }
}

export default Tests
