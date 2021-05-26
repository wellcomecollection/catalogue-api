import { SearchTemplateSource } from '../../services/search-templates'
import { Test } from '../../types'

const filterCaseRatings = (
  test: Test,
  { query }: SearchTemplateSource
): SearchTemplateSource => {
  // To avoid running exceptionally long recall queries for our negative
  // examples, we intercept the template and add a filter to only include
  // results from the set of target IDs. This should have no effect on the
  // final result, as explained in the comment below.
  const targetIds = test.cases.flatMap((x) => x.ratings)

  return {
    query: {
      bool: {
        must: [query],
        filter: { terms: { _id: targetIds } },
      },
    },
  }
}

export { filterCaseRatings }
