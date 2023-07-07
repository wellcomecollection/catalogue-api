package weco.api.search.rest

import akka.http.scaladsl.model.Uri
import io.circe.Json
import io.circe.generic.extras.JsonKey
import weco.api.search.json.CatalogueJsonUtil
import weco.api.search.models._
import weco.api.search.models.display.{
  DisplayImageAggregations,
  DisplayWorkAggregations
}
import weco.api.search.models.index.{IndexedImage, IndexedWork}
import weco.api.search.models.request.{
  MultipleImagesIncludes,
  WorkAggregationRequest,
  WorksIncludes
}
import weco.api.search.rest

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

object DisplayResultList extends CatalogueJsonUtil {
  def apply(
    resultList: ResultList[IndexedWork.Visible, WorkAggregations],
    searchOptions: SearchOptions[WorkFilter, WorkAggregationRequest, _],
    includes: WorksIncludes,
    requestUri: Uri
  ): DisplayResultList[DisplayWorkAggregations] =
    rest.PaginationResponse(resultList, searchOptions, requestUri) match {
      case PaginationResponse(totalPages, prevPage, nextPage) =>
        DisplayResultList(
          pageSize = searchOptions.pageSize,
          totalPages = totalPages,
          totalResults = resultList.totalResults,
          results = resultList.results.map(_.display.withIncludes(includes)),
          prevPage = prevPage,
          nextPage = nextPage,
          aggregations = resultList.aggregations.map(
            DisplayWorkAggregations.apply(_, searchOptions.filters)
          )
        )
    }

  def apply(
    resultList: ResultList[IndexedImage, ImageAggregations],
    searchOptions: SearchOptions[ImageFilter, _, _],
    includes: MultipleImagesIncludes,
    requestUri: Uri
  ): DisplayResultList[DisplayImageAggregations] =
    rest.PaginationResponse(resultList, searchOptions, requestUri) match {
      case PaginationResponse(totalPages, prevPage, nextPage) =>
        DisplayResultList(
          pageSize = searchOptions.pageSize,
          totalPages = totalPages,
          totalResults = resultList.totalResults,
          results = resultList.results.map(_.display.withIncludes(includes)),
          prevPage = prevPage,
          nextPage = nextPage,
          aggregations = resultList.aggregations.map(
            DisplayImageAggregations.apply(_, searchOptions.filters)
          )
        )
    }
}
