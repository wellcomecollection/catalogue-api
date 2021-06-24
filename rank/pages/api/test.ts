import { NextApiRequest, NextApiResponse } from 'next'
import { badRequest, ok } from '../../responses'
import { apiRes } from '../../services/api'
import service, { decoder } from '../../services/test'
import { Decoder } from '../../types/decoder'

export default async (
  req: NextApiRequest,
  res: NextApiResponse
): Promise<void> => {
  await apiRes(req, res, service, decoder)
}
