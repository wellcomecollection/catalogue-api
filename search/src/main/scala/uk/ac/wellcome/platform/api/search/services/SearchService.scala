package uk.ac.wellcome.platform.api.search.services

import co.elastic.apm.api.Transaction
import com.sksamuel.elastic4s.{ElasticError, Hit, Index}
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import io.circe.Decoder
import uk.ac.wellcome.Tracing
import uk.ac.wellcome.platform.api.search.models.{ResultList, SearchOptions}
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait SearchService[T, VisibleT, Aggs, S <: SearchOptions[_, _, _]] {
  val elasticsearchService: ElasticsearchService
  protected val requestBuilder: ElasticsearchRequestBuilder[S]

  implicit val ec: ExecutionContext
  implicit val decoder: Decoder[T]
  implicit val decoderV: Decoder[VisibleT]

  protected def createAggregations(searchResponse: SearchResponse): Option[Aggs]

  def findById(id: CanonicalId)(index: Index): Future[Either[ElasticError, Option[T]]] =
    elasticsearchService
      .executeGet(id)(index)
      .map {
        _.map { response =>
          if (response.exists)
            Some(deserialize[T](response))
          else
            None
        }
      }

  def listOrSearch(index: Index, searchOptions: S)
  : Future[Either[ElasticError, ResultList[VisibleT, Aggs]]] =
    executeSearch(searchOptions, index)
      .map { _.map(createResultList) }

  private def createResultList(searchResponse: SearchResponse): ResultList[VisibleT, Aggs] =
    ResultList(
      results = searchResponse.hits.hits
        .map(deserialize[VisibleT])
        .toList,
      totalResults = searchResponse.totalHits.toInt,
      aggregations = createAggregations(searchResponse)
    )

  protected def deserialize[H](hit: Hit)(implicit decoder: Decoder[H]): H =
    hit.safeTo[H] match {
      case Success(work) => work
      case Failure(e) =>
        throw new RuntimeException(
          s"Unable to parse JSON($e): ${hit.sourceAsString}"
        )
    }

  /** Given a set of query options, build a SearchDefinition for Elasticsearch
    * using the elastic4s query DSL, then execute the search.
    */
  private def executeSearch(
    searchOptions: S,
    index: Index): Future[Either[ElasticError, SearchResponse]] = {
    val searchRequest = requestBuilder
      .request(searchOptions, index)
      .trackTotalHits(true)
    Tracing.currentTransaction.addQueryOptionLabels(searchOptions)
    elasticsearchService.executeSearchRequest(searchRequest)
  }

  implicit class EnhancedTransaction(transaction: Transaction) {
    def addQueryOptionLabels(searchOptions: S): Transaction = {
      transaction.setLabel("pageSize", searchOptions.pageSize)
      transaction.setLabel("pageNumber", searchOptions.pageNumber)
      transaction.setLabel("sortOrder", searchOptions.sortOrder.toString)
      transaction.setLabel(
        "sortBy",
        searchOptions.sortBy.map { _.toString }.mkString(","))
      transaction.setLabel(
        "filters",
        searchOptions.filters.map { _.toString }.mkString(","))
      transaction.setLabel(
        "aggregations",
        searchOptions.aggregations.map { _.toString }.mkString(","))
    }
  }
}
