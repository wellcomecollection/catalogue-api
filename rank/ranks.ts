import { getSearchTemplates, SearchTemplate } from './services/search-templates'
import tests from './data/tests'
import { Test } from './types/test'

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
      const templates = await getSearchTemplates('prod')
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
      const templates = await getSearchTemplates('prod')
      return templates.find((template) => template.namespace === 'images')
    },
    tests: () => {
      return tests.images
    },
  },
]

export default ranks
