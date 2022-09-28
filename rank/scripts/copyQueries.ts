import fs from 'fs'
import { p } from './utils'

fs.copyFileSync(
  p(['../../search/src/test/resources/WorksMultiMatcherQuery.json']),
  p(['../queries/WorksMultiMatcherQuery.json'])
)

fs.copyFileSync(
  p(['../../search/src/test/resources/ImagesMultiMatcherQuery.json']),
  p(['../queries/ImagesMultiMatcherQuery.json'])
)
