package uk.ac.wellcome.api.display.models

sealed trait ImageAggregationRequest

object ImageAggregationRequest {
  case object License extends ImageAggregationRequest
  case object SourceContributorAgents extends ImageAggregationRequest
  case object SourceGenres extends ImageAggregationRequest
}
