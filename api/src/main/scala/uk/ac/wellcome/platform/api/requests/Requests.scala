package uk.ac.wellcome.platform.api.requests

import java.time.LocalDate

import com.twitter.finagle.http.Request
import com.twitter.finatra.request.{QueryParam, RouteParam}
import com.twitter.finatra.validation.{Max, Min}
import uk.ac.wellcome.display.models.{
  AggregationsRequest,
  SortingOrder,
  SortsRequest,
  V2WorksIncludes,
  WorksIncludes
}

sealed trait ApiRequest {
  val request: Request
}

trait MultipleResultsRequest[W <: WorksIncludes] extends ApiRequest {
  val page: Int
  val pageSize: Option[Int]
  val include: Option[W]
  val query: Option[String]
  val aggregations: Option[AggregationsRequest]
  val sort: Option[SortsRequest]
  val sortOrder: Option[SortingOrder]
  val _index: Option[String]
  val _queryType: Option[String]
  val request: Request
}

case class V2MultipleResultsRequest(
  @Min(1) @QueryParam page: Int = 1,
  @Min(1) @Max(100) @QueryParam pageSize: Option[Int],
  @QueryParam include: Option[V2WorksIncludes],
  @QueryParam query: Option[String],
  @QueryParam workType: Option[String],
  @QueryParam("items.locations.locationType") itemLocationType: Option[String],
  @QueryParam("production.dates.from") productionDateFrom: Option[LocalDate],
  @QueryParam("production.dates.to") productionDateTo: Option[LocalDate],
  @QueryParam language: Option[String],
  @QueryParam("genres.label") genre: Option[String],
  @QueryParam("subjects.label") subject: Option[String],
  @QueryParam("sortOrder") sortOrder: Option[SortingOrder],
  @QueryParam() aggregations: Option[AggregationsRequest],
  @QueryParam() sort: Option[SortsRequest],
  @QueryParam _index: Option[String],
  @QueryParam _queryType: Option[String],
  request: Request
) extends MultipleResultsRequest[V2WorksIncludes]

trait SingleWorkRequest[W <: WorksIncludes] {
  val id: String
  val include: Option[W]
  val _index: Option[String]
  val request: Request
}

case class V2SingleWorkRequest(
  @RouteParam id: String,
  @QueryParam include: Option[V2WorksIncludes],
  @QueryParam _index: Option[String],
  request: Request
) extends SingleWorkRequest[V2WorksIncludes]
