import { Env, Index, Namespace } from '../types/searchTemplate'
import { GetServerSideProps, NextPage } from 'next'

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
    env: Env
  }
  indices: Index[]
}

export const getServerSideProps: GetServerSideProps<Props> = async ({
  query: qs,
  req,
}) => {
  const indices = await listIndices()

  const query = qs.query ? qs.query.toString() : ''
  const env = qs.env ? qs.env.toString() : 'local'
  const index = qs.env ? qs.index.toString() : indices[0]

  let data: SearchResponse = {
    took: 0,
    hits: { total: { value: 0, relation: 'eq' }, hits: [], max_score: 0 },
  }
  if (query) {
    const { origin } = absoluteUrl(req)
    const reqQs = Object.entries({ query, index, env })
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
          env,
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
        env={search.env}
        index={search.index}
        indices={indices}
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
