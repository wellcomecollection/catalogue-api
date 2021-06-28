import { conditionalExpression } from '@babel/types'
import tests from '../data/tests'
import {
  getRemoteTemplates,
  SearchTemplate,
} from '../services/search-templates'
import testService from '../services/test'
import { getNamespaceFromIndexName } from '../types/namespace'

global.fetch = require('node-fetch')
const { works, images } = tests

let searchTemplates: SearchTemplate[] = beforeAll(async () => {
  searchTemplates = await getRemoteTemplates('prod')
})

test.each(works)('$id', async (test) => {
  const template = searchTemplates.find(
    (template) => getNamespaceFromIndexName(template.index) === 'works'
  )
  const result = await testService({
    namespace: template.namespace,
    testId: test.id,
    env: 'prod',
    index: template.index,
  })

  console.info(result)
})
