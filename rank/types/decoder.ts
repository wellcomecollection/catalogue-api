import { ParsedUrlQuery } from 'querystring'

export type Decoder<Props> = (q: ParsedUrlQuery) => Props
