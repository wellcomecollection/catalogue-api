package weco.api.search.rest

import akka.http.scaladsl.model.Uri
import io.circe.Json
import io.circe.generic.extras.JsonKey
import io.circe.syntax._
import weco.api.search.models._
import weco.api.search.rest
import weco.catalogue.display_model.models._
import weco.catalogue.display_model.models.Implicits._
import weco.catalogue.internal_model.image.{Image, ImageState}
import weco.catalogue.internal_model.work.{Work, WorkState}

case class DisplayResultList[DisplayAggs](
  @JsonKey("type") ontologyType: String = "ResultList",
  pageSize: Int,
  totalPages: Int,
  totalResults: Int,
  results: List[Json],
  prevPage: Option[String] = None,
  nextPage: Option[String] = None,
  aggregations: Option[DisplayAggs] = None
)

object DisplayResultList {
  def apply(
    resultList: ResultList[Work.Visible[WorkState.Indexed], WorkAggregations],
    searchOptions: SearchOptions[_, WorkAggregationRequest, _],
    includes: WorksIncludes,
    requestUri: Uri
  ): DisplayResultList[DisplayWorkAggregations] =
    rest.PaginationResponse(resultList, searchOptions, requestUri) match {
      case PaginationResponse(totalPages, prevPage, nextPage) => {
        val results = resultList.results.map(DisplayWork(_, includes))
          .map(_.asJson)

        DisplayResultList(
          pageSize = searchOptions.pageSize,
          totalPages = totalPages,
          totalResults = resultList.totalResults,
          results = results,
          prevPage = prevPage,
          nextPage = nextPage,
          aggregations = resultList.aggregations.map(
            DisplayWorkAggregations.apply(_, searchOptions.aggregations)
          )
        )
      }
    }

  def apply(
    resultList: ResultList[Image[ImageState.Indexed], ImageAggregations],
    searchOptions: SearchOptions[_, _, _],
    includes: MultipleImagesIncludes,
    requestUri: Uri
  ): DisplayResultList[DisplayImageAggregations] =
    rest.PaginationResponse(resultList, searchOptions, requestUri) match {
      case PaginationResponse(totalPages, prevPage, nextPage) => {
        val results = resultList.results.map(DisplayImage(_, includes))
          .map(_.asJson)

        DisplayResultList(
          pageSize = searchOptions.pageSize,
          totalPages = totalPages,
          totalResults = resultList.totalResults,
          results = results,
          prevPage = prevPage,
          nextPage = nextPage,
          aggregations =
            resultList.aggregations.map(DisplayImageAggregations.apply)
        )
      }
    }
}
