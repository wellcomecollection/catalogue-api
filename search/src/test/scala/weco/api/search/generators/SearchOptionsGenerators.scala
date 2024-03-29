package weco.api.search.generators

import weco.api.search.models.request.{
  SortRequest,
  SortingOrder,
  WorkAggregationRequest
}
import weco.api.search.models.{SearchQuery, WorkFilter, WorkSearchOptions}

trait SearchOptionsGenerators {
  def createWorksSearchOptionsWith(
    filters: List[WorkFilter] = Nil,
    pageSize: Int = 10,
    pageNumber: Int = 1,
    aggregations: List[WorkAggregationRequest] = Nil,
    sort: List[SortRequest] = Nil,
    sortOrder: SortingOrder = SortingOrder.Ascending,
    searchQuery: Option[SearchQuery] = None
  ): WorkSearchOptions =
    WorkSearchOptions(
      filters = filters,
      pageSize = pageSize,
      pageNumber = pageNumber,
      aggregations = aggregations,
      sortBy = sort,
      sortOrder = sortOrder,
      searchQuery = searchQuery
    )

  def createWorksSearchOptions: WorkSearchOptions =
    createWorksSearchOptionsWith()
}
