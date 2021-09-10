import { Query } from '../../types/searchTemplate'
import { Test } from '../../types/test'

const filterCaseRatings = (test: Test, query: Query): Query => {
  // To avoid running exceptionally long recall queries for our negative
  // examples, we intercept the template and add a filter to only include
  // results from the set of target IDs. This should have no effect on the
  // final result
  const targetIds = test.cases.flatMap((x) => x.ratings)
  return {
    bool: {
      must: [query],
      filter: { terms: { _id: targetIds } },
    },
  }
}

export { filterCaseRatings }
