import { ParsedUrlQuery } from 'querystring'

type QueryValue = ParsedUrlQuery[keyof ParsedUrlQuery]
type Decoder<Props> = (q: ParsedUrlQuery) => Props

function decodeString(q: ParsedUrlQuery, key: string): string {
  const val = q[key]
  if (!val) throw Error(`Missing value for ${key}`)
  return val.toString()
}

export { decodeString }
export type { Decoder, QueryValue }
