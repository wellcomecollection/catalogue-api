import fs from 'fs'
import { p } from './utils'

fs.copyFileSync(
  p(['../../search/src/test/resources/WorksMultiMatcherQuery.json']),
  p(['../public/WorksMultiMatcherQuery.json'])
)

fs.copyFileSync(
  p(['../../search/src/test/resources/ImagesMultiMatcherQuery.json']),
  p(['../public/ImagesMultiMatcherQuery.json'])
)
