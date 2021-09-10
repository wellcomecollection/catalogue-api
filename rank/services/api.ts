import { NextApiRequest, NextApiResponse } from 'next'

import { Decoder } from './decoder'

export function ok(res: NextApiResponse, content: unknown): void {
  res.statusCode = 200
  res.setHeader('Content-Type', 'application/json')
  res.end(JSON.stringify(content))
}

export function badRequest(res: NextApiResponse, err: Error): void {
  res.statusCode = 400
  res.setHeader('Content-Type', 'application/json')
  res.end(
    JSON.stringify({
      errorType: 'http',
      httpStatus: 400,
      label: 'Bad request',
      description: `${err.message}`,
    })
  )
}

export function notFound(res: NextApiResponse, req: NextApiRequest): void {
  res.statusCode = 404
  res.setHeader('Content-Type', 'application/json')
  res.end(
    JSON.stringify({
      errorType: 'http',
      httpStatus: 404,
      label: 'Not Found',
      description: `Page not found for URL ${req.url}`,
    })
  )
}

async function apiRes<Props>(
  req: NextApiRequest,
  res: NextApiResponse,
  service: (props?: Props) => Promise<unknown>,
  decoder?: Decoder<Props>
) {
  try {
    const props = decoder ? decoder(req.query) : undefined
    const serviceRes = props ? await service(props) : await service()
    ok(res, serviceRes)
  } catch (err) {
    badRequest(res, err)
    throw err
  }
}

export { apiRes }
