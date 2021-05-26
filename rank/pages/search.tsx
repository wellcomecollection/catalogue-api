import { FunctionComponent, useState } from 'react'
import { GetServerSideProps, NextPage } from 'next'

import { Hit as HitType } from '../services/elasticsearch'
import Link from 'next/link'
import QueryForm from '../components/QueryForm'
import { ApiResponse as SearchApiResponse } from './api/search'
import absoluteUrl from 'next-absolute-url'

type SearchProps = {
  query?: string
  rankId?: string
}
type Props = {
  data: SearchApiResponse
  search: SearchProps
}

export const getServerSideProps: GetServerSideProps<Props> = async ({
  query: qs,
  req,
}) => {
  const query = qs.query ? qs.query.toString() : undefined
  const rankId = qs.rankId ? qs.rankId.toString() : undefined
  const { origin } = absoluteUrl(req)
  const reqQs = Object.entries({ query, rankId })
    .filter(([, v]) => Boolean(v))
    .map(([k, v]) => `${k}=${encodeURIComponent(v)}`)
    .join('&')

  const data: SearchApiResponse = await fetch(
    `${origin}/api/search?${reqQs}`
  ).then((res) => res.json())

  return {
    props: {
      data,
      search: JSON.parse(
        JSON.stringify({
          query,
          rankId,
        })
      ),
    },
  }
}

type RankEvalStatusProps = {
  pass: boolean
}
const RankEvalStatus: FunctionComponent<RankEvalStatusProps> = ({ pass }) => {
  return (
    <div
      className={`w-5 h-5 mr-2 rounded-full bg-${pass ? 'green' : 'red'}-200`}
    >
      <span className="sr-only">{pass ? 'pass' : 'fail'}</span>
    </div>
  )
}

type HitProps = { hit: HitType }
const Hit: FunctionComponent<HitProps> = ({ hit }) => {
  const [showExplanation, setShowExplanation] = useState(false)
  const title =
    hit._source.source?.canonicalWork.data.title ?? hit._source.data?.title
  return (
    <>
      <h2 className="mt-5 text-xl border-t-4">{title}</h2>
      <div onClick={() => setShowExplanation(!showExplanation)}>
        Score: {hit._score}
      </div>
      {showExplanation && (
        <pre>{JSON.stringify(hit._explanation, null, 2)}</pre>
      )}
      {hit.highlight && (
        <>
          <h3 className="text-lg font-bold mt-2">Matches</h3>
          {hit.matched_queries && (
            <div>Queries: {hit.matched_queries.join(', ')}</div>
          )}
          <div>
            {Object.entries(hit.highlight).map(([key, highlight]) => {
              return (
                <div key={key}>
                  <h3 className="font-bold">{key}</h3>
                  {highlight.map((text) => (
                    <div
                      key={key}
                      dangerouslySetInnerHTML={{
                        __html: text,
                      }}
                    />
                  ))}
                </div>
              )
            })}
          </div>
        </>
      )}
    </>
  )
}

type ResultProps = {
  search: SearchProps
  result: SearchApiResponse['results'][number]
}
const Result: FunctionComponent<ResultProps> = ({ result, search }) => {
  const [showRankEval, setShowRankEval] = useState(true)

  return (
    <div className="mt-5">
      <button
        type="button"
        className={`flex flex-auto items-center mr-2 mb-2 p-2 bg-indigo-${
          showRankEval ? '100' : '200'
        } rounded-full`}
        onClick={() => setShowRankEval(!showRankEval)}
      >
        <RankEvalStatus pass={result.pass} />
        {result.label}
      </button>
      {showRankEval && (
        <div className="flex flex-wrap">
          {Object.entries(result.results).map(
            (
              [
                key,
                {
                  query,
                  result: { pass },
                },
              ],
              i
            ) => (
              <Link
                href={{
                  pathname: '/search',
                  query: JSON.parse(
                    JSON.stringify({
                      query,
                      rankId: search.rankId,
                    })
                  ),
                }}
                key={i}
              >
                <a className="flex flex-auto items-center mr-2 mb-2 p-2 bg-indigo-200 rounded-full">
                  <RankEvalStatus pass={pass} />
                  <div>{query}</div>
                </a>
              </Link>
            )
          )}
        </div>
      )}
    </div>
  )
}

const Search: NextPage<Props> = ({ data, search }) => {
  return (
    <>
      <QueryForm query={search.query} rankId={search.rankId} />

      <h1 className="text-4xl font-bold">Tests</h1>
      {data.results.map((result, i) => (
        <Result key={i} result={result} search={search} />
      ))}
      <h1 className="text-4xl font-bold">Hits</h1>
      <ul>
        {data.hits.hits.map((hit) => (
          <li key={hit._id}>
            <Hit hit={hit} />
          </li>
        ))}
      </ul>
    </>
  )
}

export default Search
