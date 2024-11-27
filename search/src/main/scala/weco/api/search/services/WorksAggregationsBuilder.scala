package weco.api.search.services

import com.sksamuel.elastic4s.requests.searches.queries.Query
import weco.api.search.models.{
  AvailabilitiesFilter,
  ContributorsFilter,
  FormatFilter,
  GenreFilter,
  LanguagesFilter,
  LicenseFilter,
  Pairable,
  SubjectLabelFilter,
  WorkFilter
}
import weco.api.search.models.request.WorkAggregationRequest
import weco.api.search.services.WorksRequestBuilder.buildWorkFilterQuery

object WorksAggregationsBuilder
    extends AggregationsBuilder[WorkAggregationRequest, WorkFilter] {
  override def getAggregationParams(
    aggReq: WorkAggregationRequest
  ): AggregationParams =
    aggReq match {
      // Note: we want these aggregations to return every possible value, so we
      // want this to be as many formats as we support in the catalogue pipeline.
      //
      // At time of writing (May 2022), we have 23 different formats; I've used
      // 30 here so we have some headroom if we add new formats in future.
      case WorkAggregationRequest.Format =>
        AggregationParams(
          "format",
          "aggregatableValues.workType",
          30,
          AggregationType.LabeledIdAggregation
        )

      case WorkAggregationRequest.ProductionDate =>
        AggregationParams(
          "productionDates",
          "aggregatableValues.production.dates",
          10,
          AggregationType.LabeledIdAggregation
        )

      case WorkAggregationRequest.Genre =>
        AggregationParams(
          "genres",
          "aggregatableValues.genres",
          20,
          AggregationType.LabeledIdAggregation
        )

      case WorkAggregationRequest.Subject =>
        AggregationParams(
          "subjects",
          "aggregatableValues.subjects",
          20,
          AggregationType.LabeledIdAggregation
        )

      case WorkAggregationRequest.Contributor =>
        AggregationParams(
          "contributors",
          "aggregatableValues.contributors.agent",
          20,
          AggregationType.LabeledIdAggregation
        )

      case WorkAggregationRequest.Languages =>
        AggregationParams(
          "languages",
          "aggregatableValues.languages",
          200,
          AggregationType.LabeledIdAggregation
        )

      // Note: we want these aggregations to return every possible value, so we
      // want this to be as many licenses as we support in the catalogue pipeline.
      //
      // At time of writing (May 2022), we have 11 different licenses; I've used
      // 20 here so we have some headroom if we add new licenses in future.
      case WorkAggregationRequest.License =>
        AggregationParams(
          "license",
          "aggregatableValues.items.locations.license",
          20,
          AggregationType.LabeledIdAggregation
        )

      // Note: we want these aggregations to return every possible value, so we
      // want this to be as many availabilities as we support in the catalogue pipeline.
      //
      // At time of writing (May 2022), we have 3 different availabilities; I've used
      // 10 here so we have some headroom if we add new ones in future.
      case WorkAggregationRequest.Availabilities =>
        AggregationParams(
          "availabilities",
          "aggregatableValues.availabilities",
          10,
          AggregationType.LabeledIdAggregation
        )
    }

  override def pairedAggregationRequests(
    filter: WorkFilter with Pairable
  ): List[WorkAggregationRequest] =
    filter match {
      case _: FormatFilter       => List(WorkAggregationRequest.Format)
      case _: LanguagesFilter    => List(WorkAggregationRequest.Languages)
      case _: GenreFilter        => List(WorkAggregationRequest.Genre)
      case _: SubjectLabelFilter => List(WorkAggregationRequest.Subject)
      case _: ContributorsFilter => List(WorkAggregationRequest.Contributor)
      case _: LicenseFilter      => List(WorkAggregationRequest.License)
      case _: AvailabilitiesFilter =>
        List(WorkAggregationRequest.Availabilities)
    }

  override def buildFilterQuery: PartialFunction[WorkFilter, Query] =
    buildWorkFilterQuery
}
