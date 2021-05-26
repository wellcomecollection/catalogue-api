import { NextApiRequest, NextApiResponse } from 'next'
import { notFound, ok } from '../../responses'
import { getNamespace } from '../../types'

const decoder = (q: NextApiRequest['query']) => ({
  namespace: getNamespace(q.namespace),
})

export default async (
  req: NextApiRequest,
  res: NextApiResponse
): Promise<void> => {
  const { namespace } = decoder(req.query)

  if (!namespace) {
    notFound(res, req)
  } else {
    const [index, query] = await Promise.all([
      await import(`../../data/indices/${namespace}`),
      await import(`../../data/queries/${namespace}`),
    ])

    ok(res, { index, query })
  }
}
