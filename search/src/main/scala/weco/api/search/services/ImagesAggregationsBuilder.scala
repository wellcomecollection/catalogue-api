package weco.api.search.services

import com.sksamuel.elastic4s.requests.searches.queries.Query
import weco.api.search.models.request.ImageAggregationRequest
import weco.api.search.models._
import weco.api.search.services.ImagesRequestBuilder.buildImageFilterQuery

object ImagesAggregationsBuilder
    extends AggregationsBuilder[ImageAggregationRequest, ImageFilter] {

  private def getSourceGenreParams(aggregationType: AggregationType) =
    AggregationParams(
      "sourceGenres",
      "aggregatableValues.source.genres",
      20,
      aggregationType
    )

  private def getSourceSubjectParams(aggregationType: AggregationType) =
    AggregationParams(
      "sourceSubjects",
      "aggregatableValues.source.subjects",
      20,
      aggregationType
    )

  private def getSourceContributorParams(aggregationType: AggregationType) =
    AggregationParams(
      "sourceContributorAgents",
      "aggregatableValues.source.contributors.agent",
      20,
      aggregationType
    )

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

      case ImageAggregationRequest.SourceContributorAgentsLabel =>
        getSourceContributorParams(AggregationType.LabelOnlyAggregation)

      case ImageAggregationRequest.SourceContributorAgentsId =>
        getSourceContributorParams(AggregationType.LabeledIdAggregation)

      case ImageAggregationRequest.SourceGenresLabel =>
        getSourceGenreParams(AggregationType.LabelOnlyAggregation)

      case ImageAggregationRequest.SourceGenresId =>
        getSourceGenreParams(AggregationType.LabeledIdAggregation)

      case ImageAggregationRequest.SourceSubjectsLabel =>
        getSourceSubjectParams(AggregationType.LabelOnlyAggregation)

      case ImageAggregationRequest.SourceSubjectsId =>
        getSourceSubjectParams(AggregationType.LabeledIdAggregation)
    }

  override def pairedAggregationRequests(
    filter: ImageFilter with Pairable
  ): List[ImageAggregationRequest] =
    filter match {
      case _: LicenseFilter => List(ImageAggregationRequest.License)
      case _: ContributorsLabelFilter =>
        List(ImageAggregationRequest.SourceContributorAgentsLabel)
      case _: ContributorsIdFilter =>
        List(ImageAggregationRequest.SourceContributorAgentsId)
      case _: GenreLabelFilter        => List(ImageAggregationRequest.SourceGenresLabel)
      case _: GenreIdFilter => List(ImageAggregationRequest.SourceGenresId)
      case _: SubjectLabelFilter =>
        List(ImageAggregationRequest.SourceSubjectsLabel)
      case _: SubjectIdFilter =>
        List(ImageAggregationRequest.SourceSubjectsId)
    }
  override def buildFilterQuery: PartialFunction[ImageFilter, Query] =
    buildImageFilterQuery
}
