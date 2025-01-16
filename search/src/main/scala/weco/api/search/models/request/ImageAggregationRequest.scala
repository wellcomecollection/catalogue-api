package weco.api.search.models.request

sealed trait ImageAggregationRequest

object ImageAggregationRequest {
  case object License extends ImageAggregationRequest
  case object SourceContributorAgentsLabel extends ImageAggregationRequest
  case object SourceContributorAgentsId extends ImageAggregationRequest
  case object SourceGenresLabel extends ImageAggregationRequest
  case object SourceGenresId extends ImageAggregationRequest
  case object SourceSubjectsLabel extends ImageAggregationRequest
  case object SourceSubjectsId extends ImageAggregationRequest
}
