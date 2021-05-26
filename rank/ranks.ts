import { getSearchTemplates, SearchTemplate } from './services/search-templates'
import { Namespace, Test } from './types'
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
  {
    id: 'works-with-search-fields',
    label: 'Works (search fields)',
    searchTemplate: async () => {
      const query = await import(
        './data/queries/works-with-search-fields'
      ).then((m) => m.default)

      const template = {
        id: 'works-with-search-fields',
        index: 'works-with-search-fields',
        namespace: 'works' as Namespace,
        source: { query },
      }

      return template
    },
    tests: () => {
      return tests.works
    },
  },
]

export default ranks
