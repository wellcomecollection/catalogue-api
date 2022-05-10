package weco.api.search.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.aggs.TermsAggregation
import io.circe.{Decoder, HCursor}
import weco.api.search.elasticsearch.{ElasticsearchError, ElasticsearchService}
import weco.api.search.json.CatalogueJsonUtil
import weco.api.search.models.index.IndexedWork
import weco.api.search.models.request.WorksIncludes
import weco.api.search.models.{
  AggregationBucket,
  ElasticAggregations,
  WorkAggregations,
  WorkSearchOptions
}
import weco.catalogue.internal_model.Implicits

import scala.concurrent.{ExecutionContext, Future}

class WorksService(val elasticsearchService: ElasticsearchService)(
  implicit
  val ec: ExecutionContext
) extends SearchService[
      IndexedWork,
      IndexedWork.Visible,
      WorkAggregations,
      WorkSearchOptions
    ]
    with ElasticAggregations
    with CatalogueJsonUtil {

  implicit val decoder: Decoder[IndexedWork] =
    (c: HCursor) =>
      Implicits._decWorkIndexed.apply(c).map {
        IndexedWork(_)
      }

  implicit val decoderV: Decoder[IndexedWork.Visible] =
    (c: HCursor) =>
      Implicits._decWorkVisibleIndexed.apply(c).map { work =>
        IndexedWork.Visible(work.asJson(WorksIncludes.all))
      }

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
