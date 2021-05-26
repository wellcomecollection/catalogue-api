import { GetServerSideProps, NextPage } from 'next'

import { ApiResponse as EvalApiResponse } from './api/eval'
import Head from 'next/head'
import absoluteUrl from 'next-absolute-url'

type Props = {
  data: EvalApiResponse
  search: {
    queryId?: string
  }
}

export const getServerSideProps: GetServerSideProps<Props> = async ({
  query: qs,
  req,
}) => {
  const queryId = qs.queryId ? qs.queryId.toString() : undefined
  const { origin } = absoluteUrl(req)
  const resp = await fetch(`${origin}/api/eval?env=prod`)
  const data: EvalApiResponse = await resp.json()

  return {
    props: {
      data,
      search: JSON.parse(
        JSON.stringify({
          queryId,
        })
      ),
    },
  }
}

function scoreToEmoji(pass: boolean): string {
  return pass ? '✅' : '❌'
}

type ResultComponentProps = {
  result: EvalApiResponse['results'][number]
}

const ResultsComponent = ({ result }: ResultComponentProps) => {
  const { namespace } = result
  return (
    <div className="py-4 font-mono">
      <h2 className="text-2xl font-bold">
        {scoreToEmoji(result.pass)} {result.label} in {result.namespace}
      </h2>

      <h3 className="font-bold">Queries:</h3>
      <ul>
        {Object.entries(result.results).map(
          ([key, { query, result, description }]) => {
            const encodedQuery = encodeURIComponent(query)
            const searchURL = `https://wellcomecollection.org/${namespace}?query=${encodedQuery}`
            return (
              <li key={key} className="py-1">
                {scoreToEmoji(result.pass)} <a href={searchURL}>{query}</a>{' '}
                <p className="text-gray-500 text-sm">{description || null}</p>
              </li>
            )
          }
        )}
      </ul>
    </div>
  )
}

const Index: NextPage<Props> = ({ data: { results } }: Props) => {
  return (
    <>
      <Head>
        <title>Relevancy ranking evaluation | Wellcome Collection</title>
      </Head>

      <h1 className="text-4xl font-bold">Rank eval</h1>

      <p className="py-2">
        When someone runs a search on{' '}
        <a href="https://wellcomecollection.org/works">
          wellcomecollection.org
        </a>
        , we transform their search terms into some structured json. That json
        forms the <i>query</i> which is run against our data in elasticsearch.
        <br />
        We update the structure of our queries periodically to improve the
        relevance of our search results.
        <br />
        Every time we update a query, we test it against a set of known search
        terms, making sure that we{"'"}re always showing people the right stuff.
        <br />
        You can{' '}
        <a href="https://api.wellcomecollection.org/catalogue/v2/search-templates.json">
          see the current candidate search queries here
        </a>
        .
      </p>

      {results.map((result) => (
        <ResultsComponent result={result} key={result.label} />
      ))}
    </>
  )
}

export default Index
