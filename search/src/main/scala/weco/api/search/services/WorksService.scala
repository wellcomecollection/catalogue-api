package weco.api.search.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.aggs.TermsAggregation
import io.circe.Decoder
import io.circe.generic.extras.semiauto._
import weco.api.search.elasticsearch.{ElasticsearchError, ElasticsearchService}
import weco.api.search.json.CatalogueJsonUtil
import weco.api.search.models.index.IndexedWork
import weco.api.search.models.{
  ElasticAggregations,
  WorkAggregations,
  WorkSearchOptions
}
import weco.json.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

class WorksService(val elasticsearchService: ElasticsearchService)(
  implicit
  val ec: ExecutionContext)
    extends SearchService[
      IndexedWork,
      IndexedWork.Visible,
      WorkAggregations,
      WorkSearchOptions
    ]
    with ElasticAggregations
    with CatalogueJsonUtil {

  implicit val decoder: Decoder[IndexedWork] = deriveConfiguredDecoder
  implicit val decoderV: Decoder[IndexedWork.Visible] = deriveConfiguredDecoder

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
          TermsAggregation("workType")
            .field("type")
        )
    )

    searchResponse.map {
      case Right(resp) => {
        val workTypeAggregation = resp.aggregations.getAgg("workType").get
        val workTypeBuckets = workTypeAggregation
          .data("buckets")
          .asInstanceOf[List[Map[String, Any]]]

        Right(
          workTypeBuckets
            .map(
              bucket =>
                bucket("key").asInstanceOf[String] -> bucket("doc_count")
                  .asInstanceOf[Int])
            .toMap
        )
      }
      case Left(err) => Left(err)
    }
  }
}
