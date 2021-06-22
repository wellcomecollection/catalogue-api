import { NextApiRequest, NextApiResponse } from 'next'
import { ParsedUrlQuery } from 'querystring'
import { badRequest, ok } from '../../responses'
import service, { Props } from '../../services/test'
import { getNamespace } from '../../types'
import { Decoder, decodeString } from '../../types/decoder'

export const decoder: Decoder<Props> = (q: ParsedUrlQuery) => ({
  index: decodeString(q, 'index'),
  testId: decodeString(q, 'testId'),
  queryId: decodeString(q, 'queryId'),
  namespace: getNamespace(q.namespace),
})

async function apiRes<Props>(
  req: NextApiRequest,
  res: NextApiResponse,
  decoder: Decoder<Props>,
  service: (props: Props) => Promise<unknown>
) {
  try {
    const props = decoder(req.query)
    const serviceRes = await service(props)
    ok(res, serviceRes)
  } catch (err) {
    badRequest(res, err)
  }
}

export default async (
  req: NextApiRequest,
  res: NextApiResponse
): Promise<void> => {
  await apiRes(req, res, decoder, service)
}
