import { NextApiRequest, NextApiResponse } from 'next'

export function ok(res: NextApiResponse, content: unknown): void {
  res.statusCode = 200
  res.setHeader('Content-Type', 'application/json')
  res.end(JSON.stringify(content))
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
