package weco.api.search.services

import com.sksamuel.elastic4s.requests.searches.queries.Query
import weco.api.search.models.request.ImageAggregationRequest
import weco.api.search.models._
import weco.api.search.services.ImagesRequestBuilder.buildImageFilterQuery

object ImagesAggregationsBuilder
    extends AggregationsBuilder[ImageAggregationRequest, ImageFilter] {
  override def getAggregationParams(
    aggReq: ImageAggregationRequest
  ): AggregationParams =
    aggReq match {
      // Note: we want these aggregations to return every possible value, so we
      // want this to be as many licenses as we support in the catalogue pipeline.
      //
      // At time of writing (May 2022), we have 11 different licenses; I've used
      // 20 here so we have some headroom if we add new licenses in future.
      case ImageAggregationRequest.License =>
        AggregationParams(
          "license",
          "aggregatableValues.locations.license",
          20,
          AggregationType.LabeledIdAggregation
        )

      case ImageAggregationRequest.SourceContributorAgents =>
        AggregationParams(
          "sourceContributorAgents",
          "aggregatableValues.source.contributors.agent",
          20,
          AggregationType.LabeledIdAggregation
        )

      case ImageAggregationRequest.SourceGenres =>
        AggregationParams(
          "sourceGenres",
          "aggregatableValues.source.genres",
          20,
          AggregationType.LabeledIdAggregation
        )

      case ImageAggregationRequest.SourceSubjects =>
        AggregationParams(
          "sourceSubjects",
          "aggregatableValues.source.subjects",
          20,
          AggregationType.LabeledIdAggregation
        )
    }

  override def pairedAggregationRequests(
    filter: ImageFilter with Pairable
  ): List[ImageAggregationRequest] =
    filter match {
      case _: LicenseFilter => List(ImageAggregationRequest.License)
      case _: ContributorsFilter =>
        List(ImageAggregationRequest.SourceContributorAgents)
      case _: GenreFilter        => List(ImageAggregationRequest.SourceGenres)
      case _: SubjectLabelFilter => List(ImageAggregationRequest.SourceSubjects)
    }
  override def buildFilterQuery: PartialFunction[ImageFilter, Query] =
    buildImageFilterQuery
}
