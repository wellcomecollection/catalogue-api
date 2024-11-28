package weco.api.search.models.request

sealed trait WorkAggregationRequest

object WorkAggregationRequest {
  case object Format extends WorkAggregationRequest

  case object ProductionDate extends WorkAggregationRequest

  case object GenreLabel extends WorkAggregationRequest

  case object GenreId extends WorkAggregationRequest


  case object SubjectLabel extends WorkAggregationRequest

  case object SubjectId extends WorkAggregationRequest

  case object ContributorLabel extends WorkAggregationRequest

  case object ContributorId extends WorkAggregationRequest

  case object Languages extends WorkAggregationRequest

  case object License extends WorkAggregationRequest

  case object Availabilities extends WorkAggregationRequest
}
