package uk.ac.wellcome.platform.api.search.services

import scala.concurrent.{ExecutionContext, Future}
import co.elastic.apm.api.Transaction
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.searches.{SearchRequest, SearchResponse}
import com.sksamuel.elastic4s.{ElasticClient, ElasticError, Index}
import grizzled.slf4j.Logging
import uk.ac.wellcome.Tracing
import uk.ac.wellcome.platform.api.search.models._
import weco.catalogue.internal_model.identifiers.CanonicalId

class ElasticsearchService(elasticClient: ElasticClient)(
  implicit ec: ExecutionContext
) extends Logging
    with Tracing {

  def executeGet(canonicalId: CanonicalId)(
    index: Index): Future[Either[ElasticError, GetResponse]] =
    withActiveTrace(elasticClient.execute {
      get(index, canonicalId.underlying)
    }).map { _.toEither }

  /** Given a set of query options, build a SearchDefinition for Elasticsearch
    * using the elastic4s query DSL, then execute the search.
    */
  def executeSearch[S <: SearchOptions[_, _, _]](
    searchOptions: S,
    requestBuilder: ElasticsearchRequestBuilder[S],
    index: Index): Future[Either[ElasticError, SearchResponse]] = {
    val searchRequest = requestBuilder
      .request(searchOptions, index)
      .trackTotalHits(true)
    Tracing.currentTransaction.addQueryOptionLabels(searchOptions)
    executeSearchRequest(searchRequest)
  }

  def executeSearchRequest(
    request: SearchRequest): Future[Either[ElasticError, SearchResponse]] =
    spanFuture(
      name = "ElasticSearch#executeSearchRequest",
      spanType = "request",
      subType = "elastic",
      action = "query"
    ) {
      debug(s"Sending ES request: ${request.show}")
      val transaction = Tracing.currentTransaction
      withActiveTrace(elasticClient.execute(request))
        .map(_.toEither)
        .map { responseOrError =>
          responseOrError.map { res =>
            transaction.setLabel("elasticTook", res.took)
            res
          }
        }
    }

  implicit class EnhancedTransaction(transaction: Transaction) {
    def addQueryOptionLabels(
      searchOptions: SearchOptions[_, _, _]): Transaction = {
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
