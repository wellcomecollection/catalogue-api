import { NextApiRequest } from 'next'

type Q = NextApiRequest['query']
type QueryValue = Q[keyof Q] | undefined
type Decoder<Props> = (q: Q) => Props

function decodeString(q: Q, key: string): string {
  if (!q[key]) throw Error(`Missing value for ${key}`)
  return q[key].toString()
}

export { decodeString }
export type { Decoder, QueryValue }
