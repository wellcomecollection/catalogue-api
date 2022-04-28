package weco.api.search.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.aggs.TermsAggregation
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Json}
import weco.json.JsonUtil._
import weco.api.search.elasticsearch.{ElasticsearchError, ElasticsearchService}
import weco.api.search.models.{AggregationBucket, ElasticAggregations, WorkAggregations, WorkSearchOptions}

import scala.concurrent.{ExecutionContext, Future}

sealed trait IndexedWork

case class Identified(canonicalId: String)

object IndexedWork {
  case class Visible(display: Json) extends IndexedWork

  case class Redirected(redirectTarget: Identified) extends IndexedWork

  case class Invisible() extends IndexedWork
  case class Deleted() extends IndexedWork
}

class WorksService(val elasticsearchService: ElasticsearchService)(
  implicit
  val ec: ExecutionContext
) extends SearchService[IndexedWork, IndexedWork.Visible, WorkAggregations, WorkSearchOptions]
    with ElasticAggregations {

  implicit val decoder: Decoder[IndexedWork] =
    deriveConfiguredDecoder
  implicit val decoderV: Decoder[IndexedWork.Visible] =
    deriveConfiguredDecoder

  override protected val requestBuilder
    : ElasticsearchRequestBuilder[WorkSearchOptions] =
    WorksRequestBuilder

  override protected def createAggregations(
    searchResponse: SearchResponse
  ): Option[WorkAggregations] =
    WorkAggregations(searchResponse)

  /** Returns a tally of all the work types in an index (e.g. Visible, Deleted). */
  def countWorkTypes(
    index: Index
  ): Future[Either[ElasticsearchError, Map[String, Int]]] = {
    val searchResponse = elasticsearchService.executeSearchRequest(
      search(index)
        .size(0)
        .aggregations(
          TermsAggregation("work_type")
            .field("type")
        )
    )

    searchResponse.map {
      case Right(resp) =>
        Right(
          resp.aggregations
            .decodeAgg[String]("work_type")
            .get
            .buckets
            .map {
              case AggregationBucket(data, count) => data -> count
            }
            .toMap
        )
      case Left(err) => Left(err)
    }
  }
}
