package uk.ac.wellcome.platform.api.search.services

import com.sksamuel.elastic4s.requests.searches.SearchResponse
import io.circe.Decoder
import weco.catalogue.internal_model.Implicits
import uk.ac.wellcome.platform.api.search.models._
import weco.api.search.elasticsearch.ElasticsearchService
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed

import scala.concurrent.ExecutionContext

class WorksService(val elasticsearchService: ElasticsearchService)(
  implicit
  val ec: ExecutionContext)
    extends SearchService[
      Work[Indexed],
      Work.Visible[Indexed],
      WorkAggregations,
      WorkSearchOptions] {

  // TODO: This isn't ideal, but it's the only way I've been able to get
  // this to compile.  We should move towards named implicits here.
  implicit val decoder: Decoder[Work[Indexed]] =
    Implicits._dec68
  implicit val decoderV: Decoder[Work.Visible[Indexed]] =
    Implicits._dec69

  override protected val requestBuilder
    : ElasticsearchRequestBuilder[WorkSearchOptions] =
    WorksRequestBuilder

  override protected def createAggregations(
    searchResponse: SearchResponse): Option[WorkAggregations] =
    WorkAggregations(searchResponse)
}
