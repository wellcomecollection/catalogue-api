import { GetServerSideProps, NextPage } from 'next'
import { Index, Namespace, QueryEnv } from '../types/searchTemplate'

import Hit from '../components/Hit'
import QueryForm from '../components/QueryForm'
import { SearchResponse } from '../types/elasticsearch'
import absoluteUrl from 'next-absolute-url'
import { listIndices } from '../services/search-templates'

type Props = {
  data: SearchResponse
  search: {
    query?: string
    namespace?: Namespace
    index?: Index
    queryEnv: QueryEnv
  }
  indices: Index[]
}

export const getServerSideProps: GetServerSideProps<Props> = async ({
  query: qs,
  req,
}) => {
  const indices = await listIndices()

  const query = qs.query ? qs.query.toString() : ''
  const queryEnv = qs.queryEnv ? qs.queryEnv.toString() : 'candidate'
  const index = qs.index ? qs.index.toString() : indices[0]

  let data: SearchResponse = null
  if (query) {
    const { origin } = absoluteUrl(req)
    const reqQs = Object.entries({ query, index, queryEnv })
      .filter(([, v]) => Boolean(v))
      .map(([k, v]) => `${k}=${encodeURIComponent(v)}`)
      .join('&')
    const url = `${origin}/api/search?${reqQs}`
    data = await fetch(url).then((res) => res.json())
  }

  return {
    props: {
      data,
      indices,
      search: JSON.parse(
        JSON.stringify({
          query,
          queryEnv,
          index,
        })
      ),
    },
  }
}

const Search: NextPage<Props> = ({ data, search, indices }) => {
  return (
    <>
      <QueryForm
        query={search.query}
        queryEnv={search.queryEnv}
        index={search.index}
        indices={indices}
      />
      <div className="mt-2 flex-grow border-t border-gray-500" />
      <ul className="mt-3 space-y-6">
        {data
          ? data.hits.hits.map((hit) => (
              <li key={hit._id}>
                <Hit hit={hit} />
              </li>
            ))
          : null}
      </ul>
    </>
  )
}

export default Search
