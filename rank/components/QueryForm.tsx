import { Env, envs } from '../types/env'
import { FC, useEffect, useState } from 'react'
import { Namespace, namespaces } from '../types/namespace'

type Props = {
  query?: string
  namespace?: Namespace
  env?: Env
}

const QueryForm: FC<Props> = (props) => {
  const [query, setQuery] = useState(props.query)
  const [namespace, setNamespace] = useState<Namespace>(props.namespace)
  const [env, setEnv] = useState<Env>(props.env)

  useEffect(() => {
    setQuery(props.query)
  }, [props.query])

  return (
    <form className="pt-2">
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
          Namespace
          <select
            className="ml-1"
            name="namespace"
            onChange={(event) =>
              setNamespace(event.currentTarget.value as Namespace)
            }
            value={namespace}
          >
            {namespaces.map((namespace) => (
              <option key={namespace} value={namespace}>
                {namespace}
              </option>
            ))}
          </select>
        </label>
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
      </div>
    </form>
  )
}

export default QueryForm
