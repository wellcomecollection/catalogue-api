import { NextApiRequest, NextApiResponse } from 'next'
import { ratingClient } from '../../services/elasticsearch'

export type Rating = {
  username: string | undefined
  workId: string
  query: string
  rating: number
  position: number
}

export default async (
  req: NextApiRequest,
  res: NextApiResponse
): Promise<void> => {
  if (req.method === 'POST') {
    const {
      body: { username, workId, query, rating, position },
    } = req

    if (
      // This is crap, but 0 is a valid value, but falsy in JS land
      [workId, query, rating, position].every(
        (val) => typeof val !== 'undefined'
      )
    ) {
      const ratingDoc: Rating = {
        username,
        workId,
        query,
        rating: parseInt(rating, 10),
        position: parseInt(position, 10),
      }

      const resp = await ratingClient.index({
        index: 'ratings',
        body: ratingDoc,
      })

      const body = resp.body

      if (!body.status) {
        res.statusCode = 200
        res.setHeader('Content-Type', 'application/json')
        res.end(JSON.stringify(ratingDoc))
      } else {
        res.statusCode = body.status
        res.setHeader('Content-Type', 'application/json')
        res.end(JSON.stringify(body))
      }
    } else {
      res.statusCode = 400
      res.setHeader('Content-Type', 'application/json')
      res.end(
        JSON.stringify({
          message: 'Bad Request',
          body: req.body,
          params: { username, workId, query, rating, position },
        })
      )
    }
  } else {
    res.statusCode = 405
    res.setHeader('Content-Type', 'application/json')
    res.end(JSON.stringify({ message: 'Method Not Allowed' }))
  }
}
