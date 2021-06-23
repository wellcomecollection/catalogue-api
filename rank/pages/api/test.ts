import { NextApiRequest, NextApiResponse } from 'next'
import { badRequest, ok } from '../../responses'
import service, { decoder } from '../../services/test'
import { Decoder } from '../../types/decoder'

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
