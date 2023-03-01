import fs from 'fs'
import { p } from './utils'

fs.copyFileSync(
  p(['../../search/src/test/resources/WorksMultiMatcherQuery.json']),
  p(['../data/queries/WorksMultiMatcherQuery.json'])
)

fs.copyFileSync(
  p(['../../search/src/test/resources/ImagesMultiMatcherQuery.json']),
  p(['../data/queries/ImagesMultiMatcherQuery.json'])
)
