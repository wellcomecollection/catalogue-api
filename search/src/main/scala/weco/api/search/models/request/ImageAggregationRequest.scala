package weco.api.search.models.request

sealed trait ImageAggregationRequest

object ImageAggregationRequest {
  case object License extends ImageAggregationRequest
  case object SourceContributorAgents extends ImageAggregationRequest
  case object SourceGenres extends ImageAggregationRequest
}
