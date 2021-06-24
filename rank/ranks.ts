import { getSearchTemplates, SearchTemplate } from './services/search-templates'
import tests from './data/tests'
import { Test } from './types/test'
import worksCandidate from './data/queries/worksCandidate'

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
    id: 'works-with-archive-descriptions',
    label: 'Works candidate',
    searchTemplate:  async () => {
      return {
        id: 'works-with-archive-descriptions',
        index: `works-with-archive-descriptions`,
        namespace: 'works',
        source: {
          query: worksCandidate,
        }
    }
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
