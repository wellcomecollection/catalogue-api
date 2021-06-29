import { NextApiRequest, NextApiResponse } from 'next'
import service, { decoder } from '../../services/test'
import { apiRes } from '../../services/api'

export default async (
  req: NextApiRequest,
  res: NextApiResponse
): Promise<void> => {
  await apiRes(req, res, service, decoder)
}
