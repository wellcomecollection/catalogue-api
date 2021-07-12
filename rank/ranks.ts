import {
  SearchTemplate,
  getLocalTemplates,
  getRemoteTemplates,
} from './services/search-templates'

import { Test } from './types/test'
import tests from './data/tests'

type Rank = {
  id: string
  label: string
  searchTemplate: () => Promise<SearchTemplate>
  tests: () => Test[]
}

const ranks: Rank[] = [
  {
    id: 'works-prod',
    label: 'Works',
    searchTemplate: async () => {
      const templates = await getRemoteTemplates('prod')
      return templates.find((template) => template.namespace === 'works')
    },
    tests: () => {
      return tests.works
    },
  },
  {
    id: 'images-prod',
    label: 'Images',
    searchTemplate: async () => {
      const templates = await getRemoteTemplates('prod')
      return templates.find((template) => template.namespace === 'images')
    },
    tests: () => {
      return tests.images
    },
  },
  {
    id: 'works-local',
    label: 'Works',
    searchTemplate: async () => {
      const templates = await getLocalTemplates({})
      return templates.find((template) => template.namespace === 'works')
    },
    tests: () => {
      return tests.works
    },
  },
  {
    id: 'images-local',
    label: 'Images',
    searchTemplate: async () => {
      const templates = await getLocalTemplates({})
      return templates.find((template) => template.namespace === 'images')
    },
    tests: () => {
      return tests.images
    },
  },
]

export default ranks
