package uk.ac.wellcome.platform.api.rest

import akka.http.scaladsl.model.Uri
import io.circe.generic.extras.JsonKey
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import io.circe.Encoder
import io.swagger.v3.oas.annotations.media.Schema
import uk.ac.wellcome.display.models._
import uk.ac.wellcome.platform.api.models._
import weco.catalogue.internal_model.image.{Image, ImageState}
import weco.catalogue.internal_model.work.{Work, WorkState}
import weco.http.json.DisplayJsonUtil._

@Schema(
  name = "ResultList",
  description = "A paginated list of results."
)
case class DisplayResultList[DisplayResult, DisplayAggs](
  @JsonKey("@context") context: String,
  @JsonKey("type") @Schema(name = "type") ontologyType: String = "ResultList",
  pageSize: Int,
  totalPages: Int,
  totalResults: Int,
  results: List[DisplayResult],
  prevPage: Option[String] = None,
  nextPage: Option[String] = None,
  aggregations: Option[DisplayAggs] = None
)

object DisplayResultList {
  implicit def encoder[R: Encoder, A: Encoder]
    : Encoder[DisplayResultList[R, A]] = deriveConfiguredEncoder

  def apply(
    resultList: ResultList[Work.Visible[WorkState.Indexed], WorkAggregations],
    searchOptions: SearchOptions[_, WorkAggregationRequest, _],
    includes: WorksIncludes,
    requestUri: Uri,
    contextUri: String)
    : DisplayResultList[DisplayWork, DisplayWorkAggregations] =
    PaginationResponse(resultList, searchOptions, requestUri) match {
      case PaginationResponse(totalPages, prevPage, nextPage) =>
        DisplayResultList(
          context = contextUri,
          pageSize = searchOptions.pageSize,
          totalPages = totalPages,
          totalResults = resultList.totalResults,
          results = resultList.results.map(DisplayWork(_, includes)),
          prevPage = prevPage,
          nextPage = nextPage,
          aggregations = resultList.aggregations.map(
            DisplayWorkAggregations.apply(_, searchOptions.aggregations)
          )
        )
    }

  def apply(
    resultList: ResultList[Image[ImageState.Indexed], ImageAggregations],
    searchOptions: SearchOptions[_, _, _],
    includes: MultipleImagesIncludes,
    requestUri: Uri,
    contextUri: String)
    : DisplayResultList[DisplayImage, DisplayImageAggregations] =
    PaginationResponse(resultList, searchOptions, requestUri) match {
      case PaginationResponse(totalPages, prevPage, nextPage) =>
        DisplayResultList(
          context = contextUri,
          pageSize = searchOptions.pageSize,
          totalPages = totalPages,
          totalResults = resultList.totalResults,
          results = resultList.results.map(DisplayImage(_, includes)),
          prevPage = prevPage,
          nextPage = nextPage,
          aggregations =
            resultList.aggregations.map(DisplayImageAggregations.apply)
        )
    }
}
