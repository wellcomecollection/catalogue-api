package weco.api.search.models

import weco.api.search.models.request.{
  ImageAggregationRequest,
  SortRequest,
  SortingOrder,
  WorkAggregationRequest
}

sealed trait SearchOptions[DocFilter, AggregationRequest] {
  val searchQuery: Option[SearchQuery]
  val filters: List[DocFilter]
  val aggregations: List[AggregationRequest]
  val sortBy: List[SortRequest]
  val sortOrder: SortingOrder
  val pageSize: Int
  val pageNumber: Int
}

case class WorkSearchOptions(
  searchQuery: Option[SearchQuery] = None,
  filters: List[WorkFilter] = Nil,
  aggregations: List[WorkAggregationRequest] = Nil,
  sortBy: List[SortRequest] = Nil,
  sortOrder: SortingOrder = SortingOrder.Ascending,
  pageSize: Int = 10,
  pageNumber: Int = 1
) extends SearchOptions[WorkFilter, WorkAggregationRequest]

case class ImageSearchOptions(
  searchQuery: Option[SearchQuery] = None,
  filters: List[ImageFilter] = Nil,
  aggregations: List[ImageAggregationRequest] = Nil,
  color: Option[RgbColor] = None,
  sortBy: List[SortRequest] = Nil,
  sortOrder: SortingOrder = SortingOrder.Ascending,
  pageSize: Int = 10,
  pageNumber: Int = 1
) extends SearchOptions[ImageFilter, ImageAggregationRequest]
