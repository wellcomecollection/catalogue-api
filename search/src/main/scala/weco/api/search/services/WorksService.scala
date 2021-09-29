package weco.api.search.services

import com.sksamuel.elastic4s.requests.searches.SearchResponse
import io.circe.Decoder
import weco.catalogue.internal_model.Implicits
import weco.api.search.elasticsearch.ElasticsearchService
import weco.api.search.models.{WorkAggregations, WorkSearchOptions}
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed

import scala.concurrent.ExecutionContext

class WorksService(val elasticsearchService: ElasticsearchService)(
  implicit
  val ec: ExecutionContext
) extends SearchService[Work[Indexed], Work.Visible[Indexed], WorkAggregations, WorkSearchOptions] {

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
}
