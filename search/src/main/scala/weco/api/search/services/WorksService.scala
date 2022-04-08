package weco.api.search.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.aggs.TermsAggregation
import io.circe.Decoder
import weco.api.search.elasticsearch.{ElasticsearchError, ElasticsearchService}
import weco.api.search.models.{
  AggregationBucket,
  ElasticAggregations,
  WorkAggregations,
  WorkSearchOptions
}
import weco.catalogue.internal_model.Implicits
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed

import scala.concurrent.{ExecutionContext, Future}

class WorksService(val elasticsearchService: ElasticsearchService)(
  implicit
  val ec: ExecutionContext
) extends SearchService[
      Work[Indexed],
      Work.Visible[Indexed],
      WorkAggregations,
      WorkSearchOptions]
    with ElasticAggregations {

  implicit val decoder: Decoder[Work[Indexed]] =
    Implicits._decWorkIndexed
  implicit val decoderV: Decoder[Work.Visible[Indexed]] =
    Implicits._decWorkVisibleIndexed

  override protected val requestBuilder
    : ElasticsearchRequestBuilder[WorkSearchOptions] =
    WorksRequestBuilder

  override protected def createAggregations(
    searchResponse: SearchResponse
  ): Option[WorkAggregations] =
    WorkAggregations(searchResponse)

  /** Returns a tally of all the work types in an index (e.g. Visible, Deleted). */
  def countWorkTypes(
    index: Index): Future[Either[ElasticsearchError, Map[String, Int]]] = {
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
