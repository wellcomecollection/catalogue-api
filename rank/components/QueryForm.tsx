import { FunctionComponent, useEffect, useState } from 'react'
import ranks from '../ranks'

type Props = {
  query?: string
  rankId?: string
}

const QueryForm: FunctionComponent<Props> = (props) => {
  const [query, setQuery] = useState(props.query)
  const [rankId, setsetRankId] = useState(props.rankId)

  useEffect(() => {
    setQuery(props.query)
  }, [props.query])

  return (
    <form className="mb-5 border-2 border-purple-400 rounded-full flex">
      <div className="flex-grow">
        <label className="p-2 mr-4 inline-block">
          Query
          <input
            className="ml-2"
            type="text"
            name="query"
            value={query}
            placeholder="What are you looking for?"
            onChange={(event) => setQuery(event.currentTarget.value)}
          />
        </label>
        <label className="p-2 mr-4 inline-block">
          Rank
          <select
            className="ml-2"
            name="rankId"
            onChange={(event) => setsetRankId(event.currentTarget.value)}
            value={rankId}
          >
            {ranks.map((rank) => (
              <option key={rank.id} value={rank.id}>
                {rank.label}
              </option>
            ))}
          </select>
        </label>
      </div>
      <div className="flex-shrink">
        <button
          className="p-2 ml-3"
          aria-label="Search catalogue"
          type="submit"
        >
          🔎
        </button>
      </div>
    </form>
  )
}

export default QueryForm
