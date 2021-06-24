import { ParsedUrlQuery } from 'querystring'
import { isNamespace, Namespace } from './namespace'

type QueryValue = ParsedUrlQuery[keyof ParsedUrlQuery]
type Decoder<Props> = (q: ParsedUrlQuery) => Props

function decodeString(q: ParsedUrlQuery, key: string): string {
  const val = q[key]
  if (!val) throw Error(`Missing value for ${key}`)
  return val.toString()
}

function decodeNamespace(v: QueryValue): Namespace {
  if (!isNamespace(v)) throw Error(`${v} is not a valid namespace`)
  return v
}

export { decodeString, decodeNamespace }
export type { Decoder, QueryValue }
