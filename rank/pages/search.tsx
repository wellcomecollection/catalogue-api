import { GetServerSideProps, NextPage } from 'next'

import { Env } from '../types/env'
import Hit from '../components/Hit'
import { Namespace } from '../types/namespace'
import QueryForm from '../components/QueryForm'
import { ApiResponse as SearchApiResponse } from './api/search'
import absoluteUrl from 'next-absolute-url'

type Props = {
  data: SearchApiResponse
  search: {
    query?: string
    namespace?: Namespace
    env: Env
  }
}

export const getServerSideProps: GetServerSideProps<Props> = async ({
  query: qs,
  req,
}) => {
  const query = qs.query ? qs.query.toString() : undefined
  const namespace = qs.namespace ? qs.namespace.toString() : undefined
  const env = qs.env ? qs.env.toString() : undefined
  const { origin } = absoluteUrl(req)
  const reqQs = Object.entries({ query, namespace, env })
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
          namespace,
          env,
        })
      ),
    },
  }
}

const Search: NextPage<Props> = ({ data, search }) => {
  return (
    <>
      <QueryForm
        query={search.query}
        namespace={search.namespace}
        env={search.env}
      />
      <div className="mt-2 flex-grow border-t border-gray-500" />
      <ul className="mt-3 space-y-6">
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
