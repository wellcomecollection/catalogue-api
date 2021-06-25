package uk.ac.wellcome.platform.api.search.generators

import uk.ac.wellcome.api.display.models.WorkAggregationRequest
import uk.ac.wellcome.platform.api.search.models.{
  SearchQuery,
  WorkFilter,
  WorkSearchOptions
}
import weco.catalogue.display_model.models.{
  SortRequest,
  SortingOrder,
  WorkAggregationRequest
}

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
