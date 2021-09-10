import { ParsedUrlQuery } from 'querystring'

export type Decoder<Props> = (q: ParsedUrlQuery) => Props

export function decodeString(q: ParsedUrlQuery, key: string): string {
  const val = q[key]
  if (!val) throw Error(`Missing value for ${key}`)
  return val.toString()
}
