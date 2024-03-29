package weco.api.search.services

import co.elastic.apm.api.Transaction
import com.sksamuel.elastic4s.{Hit, Index}
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import io.circe.Decoder
import weco.Tracing
import weco.api.search.elasticsearch.{ElasticsearchError, ElasticsearchService}
import weco.api.search.models.{ResultList, SearchOptions}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait SearchService[T, VisibleT, Aggs, S <: SearchOptions[_, _]] {
  val elasticsearchService: ElasticsearchService
  protected val requestBuilder: ElasticsearchRequestBuilder[S]

  implicit val ec: ExecutionContext
  implicit val decoder: Decoder[T]
  implicit val decoderV: Decoder[VisibleT]

  protected def createAggregations(searchResponse: SearchResponse): Option[Aggs]

  def findById(
    id: String
  )(index: Index): Future[Either[ElasticsearchError, T]] =
    elasticsearchService.findById[T](id)(index)

  def listOrSearch(
    index: Index,
    searchOptions: S
  ): Future[Either[ElasticsearchError, ResultList[VisibleT, Aggs]]] =
    executeSearch(searchOptions, index)
      .map { _.map(createResultList) }

  private def createResultList(
    searchResponse: SearchResponse
  ): ResultList[VisibleT, Aggs] =
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
    index: Index
  ): Future[Either[ElasticsearchError, SearchResponse]] = {
    val request = requestBuilder
      .request(searchOptions, index)
    Tracing.currentTransaction.addQueryOptionLabels(searchOptions)
    // This offers a choice between the two options Images and countWorkTypes
    // still use the old way.
    // Eventually, this should only return a TemplateSearchRequest.
    request match {
      case Left(search) => elasticsearchService.executeSearchRequest(search)
      case Right(template) =>
        elasticsearchService.executeTemplateSearchRequest(template)
    }

  }

  implicit class EnhancedTransaction(transaction: Transaction) {
    def addQueryOptionLabels(searchOptions: S): Transaction = {
      transaction.setLabel("pageSize", searchOptions.pageSize)
      transaction.setLabel("pageNumber", searchOptions.pageNumber)
      transaction.setLabel("sortOrder", searchOptions.sortOrder.toString)
      transaction.setLabel(
        "sortBy",
        searchOptions.sortBy.map { _.toString }.mkString(",")
      )
      transaction.setLabel(
        "filters",
        searchOptions.filters.map { _.toString }.mkString(",")
      )
      transaction.setLabel(
        "aggregations",
        searchOptions.aggregations.map { _.toString }.mkString(",")
      )
    }
  }
}
