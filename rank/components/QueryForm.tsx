import { Env, Index, envs } from '../types/searchTemplate'
import { FC, useEffect, useState } from 'react'

type Props = {
  query?: string
  index?: Index
  env?: Env
  indices: Index[]
}

const QueryForm: FC<Props> = (props) => {
  const [query, setQuery] = useState(props.query)
  const [env, setEnv] = useState<Env>(props.env)
  const [index, setIndex] = useState<Index>(props.index)

  useEffect(() => {
    setQuery(props.query)
  }, [props.query])

  return (
    <form className="pt-2 text-black">
      <div className="w-full flex text-2xl">
        <input
          className="pl-2 flex-grow h-12 rounded-l-md border-2 border-r-0 border-gray-600 focus:outline-none"
          type="text"
          name="query"
          value={query}
          placeholder="What are you looking for?"
          onChange={(event) => setQuery(event.currentTarget.value)}
        />
        <button
          className="h-12 w-16 rounded-r-md bg-gray-300 border-2 border-gray-600"
          aria-label="Search catalogue"
          type="submit"
        >
          🔎
        </button>
      </div>

      <div className="space-x-6 pt-2 pl-1">
        <label className="inline-block font-bold">
          Env
          <select
            className="ml-1"
            name="env"
            onChange={(event) => setEnv(event.currentTarget.value as Env)}
            value={env}
          >
            {envs.map((env) => (
              <option key={env} value={env}>
                {env}
              </option>
            ))}
          </select>
        </label>
        <label className="inline-block font-bold">
          Index
          <select
            className="ml-1"
            name="index"
            onChange={(event) => setIndex(event.currentTarget.value as Index)}
            value={index}
          >
            {props.indices.map((index) => (
              <option key={index} value={index}>
                {index}
              </option>
            ))}
          </select>
        </label>
      </div>
    </form>
  )
}

export default QueryForm
