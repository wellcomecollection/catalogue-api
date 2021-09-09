import { Env, Namespace, isEnv, isNamespace } from '../types/searchTemplate'

import { ParsedUrlQuery } from 'querystring'

type QueryValue = ParsedUrlQuery[keyof ParsedUrlQuery]

export function decodeString(q: ParsedUrlQuery, key: string): string {
  const val = q[key]
  if (!val) throw Error(`Missing value for ${key}`)
  return val.toString()
}

export function decodeNamespace(v: QueryValue): Namespace {
  if (!isNamespace(v)) throw Error(`${v} is not a valid namespace`)
  return v
}

export function decodeEnv(v: QueryValue): Env {
  if (!isEnv(v)) throw Error(`${v} is not a valid Env`)
  return v
}
