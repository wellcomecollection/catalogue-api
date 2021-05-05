package uk.ac.wellcome.platform.api.search.services

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import io.circe.Decoder
import com.sksamuel.elastic4s.{Hit, Index}
import com.sksamuel.elastic4s.ElasticError
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.circe._
import uk.ac.wellcome.platform.api.search.models._
import uk.ac.wellcome.models.Implicits._
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed

class WorksService(searchService: ElasticsearchService)(
  implicit ec: ExecutionContext) {

  def listOrSearchWorks(index: Index, searchOptions: WorkSearchOptions): Future[
    Either[ElasticError, ResultList[Work.Visible[Indexed], WorkAggregations]]] =
    searchService
      .executeSearch(
        searchOptions = searchOptions,
        requestBuilder = WorksRequestBuilder,
        index = index
      )
      .map { _.map(createResultList) }

  private def createResultList(searchResponse: SearchResponse)
    : ResultList[Work.Visible[Indexed], WorkAggregations] = {
    ResultList(
      results = searchResponseToWorks(searchResponse),
      totalResults = searchResponse.totalHits.toInt,
      aggregations = WorkAggregations(searchResponse)
    )
  }

  private def searchResponseToWorks(
    searchResponse: SearchResponse): List[Work.Visible[Indexed]] =
    searchResponse.hits.hits.map { hit =>
      deserialize[Work.Visible[Indexed]](hit)
    }.toList

  private def deserialize[T](hit: Hit)(implicit decoder: Decoder[T]): T =
    hit.safeTo[T] match {
      case Success(work) => work
      case Failure(e) =>
        throw new RuntimeException(
          s"Unable to parse JSON($e): ${hit.sourceAsString}"
        )
    }
}
