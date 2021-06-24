import { NextApiRequest, NextApiResponse } from 'next'
import { notFound, ok } from '../../responses'
import { decodeNamespace } from '../../types/decoder'

const decoder = (q: NextApiRequest['query']) => ({
  namespace: decodeNamespace(q.namespace),
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
      await import(`../../data/indices/${namespace}`).then((m) => m.default),
      await import(`../../data/queries/${namespace}`).then((m) => m.default),
    ])

    ok(res, { index, query })
  }
}
