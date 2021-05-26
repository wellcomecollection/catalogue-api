import { NextApiRequest, NextApiResponse } from 'next'
import { rankClient } from '../../services/elasticsearch'
import { getSearchTemplates } from '../../services/search-templates'

function getNamespace(indexName: string) {
  return indexName.replace('ccr--', '').split('-')[0]
}

type State = {
  index: string
  state: 'delete' | 'exists' | 'follow'
}

export default async (
  req: NextApiRequest,
  res: NextApiResponse
): Promise<void> => {
  const auth = process.env.CCR_MANAGE_API_KEY
  const authHeader = req.headers.authorization

  if (!auth || !authHeader || auth !== process.env.CCR_MANAGE_API_KEY) {
    res.statusCode = 401
    res.setHeader('Content-Type', 'application/json')
    res.end(
      JSON.stringify({
        statusCode: 401,
        message: 'unauthorised',
      })
    )
    return
  }

  const searchTemplates = await getSearchTemplates('prod')
  const { body: allIndices } = await rankClient.indices.get({ index: '_all' })

  const state: State[] = (
    await Promise.all(
      searchTemplates.map(async (template) => {
        const namespace = getNamespace(template.index)
        const ccrIndexName = `${template.index}`
        // we're only working with works for now, but should extend to images

        console.info(`searching for ${ccrIndexName}`)
        const { body: indexExists } = await rankClient.indices
          .exists({
            index: ccrIndexName,
          })
          .catch(() => ({
            body: false,
          }))

        // delete previous indices
        const deleteIndices = Object.keys(allIndices).filter(
          (index) =>
            ccrIndexName !== index && index.startsWith(`ccr--${namespace}`)
        )

        if (deleteIndices.length > 0) {
          console.info(`deleting ${deleteIndices}`)
          await rankClient.indices
            .delete({
              index: deleteIndices,
            })
            .catch((err) => ({ body: err }))
        } else {
          console.info(`nothing to delete`)
        }

        if (indexExists) {
          console.info(`${ccrIndexName} exists, moving on`)
          return [
            ...deleteIndices.map(
              (index) =>
                ({
                  index,
                  state: 'delete',
                } as State)
            ),
            {
              index: ccrIndexName,
              state: 'exists',
            } as State,
          ]
        }

        await rankClient.ccr.follow({
          index: ccrIndexName,
          body: {
            remote_cluster: 'catalogue',
            leader_index: template.index.replace('ccr--', ''),
            settings: {
              'index.number_of_replicas': 0,
            },
          },
        })

        return [
          ...deleteIndices.map(
            (index) =>
              ({
                index,
                state: 'delete',
              } as State)
          ),
          { index: ccrIndexName, state: 'follow' } as State,
        ]
      })
    )
  )

    .flat()
    // We can sometimes have `null` as we don't run this on all templates yet
    .filter(Boolean)

  res.statusCode = 200
  res.setHeader('Content-Type', 'application/json')
  res.end(JSON.stringify({ state }))
}
