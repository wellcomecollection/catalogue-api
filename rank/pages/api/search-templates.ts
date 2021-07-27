import { NextApiRequest, NextApiResponse } from 'next'
import { apiRes } from '../../services/api'
import service from '../../services/search-templates'

export default async (
  req: NextApiRequest,
  res: NextApiResponse
): Promise<void> => {
  await apiRes(req, res, service)
}
