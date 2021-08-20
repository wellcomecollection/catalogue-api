package weco.api.requests.services

import com.sksamuel.elastic4s.ElasticApi.fieldSort
import com.sksamuel.elastic4s.ElasticDsl.{boolQuery, search, termQuery}
import com.sksamuel.elastic4s.requests.searches.MultiSearchRequest
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import com.sksamuel.elastic4s.{ElasticClient, Index}
import weco.api.requests.models.RequestedItemWithWork
import weco.api.search.elasticsearch.{
  DocumentNotFoundError,
  ElasticsearchError,
  ElasticsearchService
}
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.identifiers.{
  CanonicalId,
  IdState,
  SourceIdentifier
}
import weco.catalogue.internal_model.work.WorkState.Indexed
import weco.catalogue.internal_model.work.{Item, Work}

import scala.concurrent.{ExecutionContext, Future}

class ItemLookup(
  elasticsearchService: ElasticsearchService,
  index: Index
)(
  implicit ec: ExecutionContext
) {

  /** Returns the SourceIdentifier of the item that corresponds to this
    * canonical ID.
    *
    */
  def byCanonicalId(
    itemId: CanonicalId
  ): Future[Either[ElasticsearchError, Item[IdState.Identified]]] = {
    val searchRequest =
      search(index)
        .query(
          boolQuery.filter(
            termQuery("data.items.id.canonicalId", itemId.underlying),
            termQuery(field = "type", value = "Visible")
          )
        )
        .size(1)

    elasticsearchService.findBySearch[Work[Indexed]](searchRequest).map {
      case Left(err) => Left(err)
      case Right(works) =>
        val item =
          works
            .flatMap { _.data.items }
            .collectFirst {
              case item @ Item(IdState.Identified(id, _, _), _, _, _)
                  if id == itemId =>
                // This .asInstanceOf[] is a no-op to help the compiler see what
                // we can see by reading the code.
                item.asInstanceOf[Item[IdState.Identified]]
            }

        item match {
          case Some(it) => Right(it)
          case None     => Left(DocumentNotFoundError(itemId))
        }
    }
  }

  def bySourceIdentifier(
    sourceIdentifiers: Seq[SourceIdentifier]
  ): Future[Seq[Either[ElasticsearchError, RequestedItemWithWork]]] = {
    val multiSearchRequest = MultiSearchRequest(
      sourceIdentifiers.map { sourceIdentifier =>
        search(index)
          .query(
            boolQuery
              .must(
                termQuery(
                  field = "type",
                  value = "Visible"
                ),
                termQuery(
                  field = "data.items.id.sourceIdentifier.value",
                  value = sourceIdentifier.value
                )
              )
          )
          .sortBy(
            fieldSort("state.sourceIdentifier.value")
              .order(SortOrder.Asc)
          )
          // We are sorting so we're only interested in the top value
          .size(1)
      }
    )

    elasticsearchService
      .findByMultiSearch[Work[Indexed]](multiSearchRequest)
      .map {
        _.zip(sourceIdentifiers).map {
          case (Right(works), srcId) =>
            works
              .flatMap { work =>
                work.data.items.map(
                  item => (work.data.title, work.state.canonicalId, item)
                )
              }
              .collect {
                case (
                    title,
                    workId,
                    item @ Item(id @ IdState.Identified(_, _, _), _, _, _)
                    ) if id.sourceIdentifier == srcId =>
                  // This .asInstanceOf[] is a no-op to help the compiler see what
                  // we can see by reading the code.
                  item.asInstanceOf[Item[IdState.Identified]]
                  RequestedItemWithWork(
                    workId = workId,
                    workTitle = title,
                    item = item.asInstanceOf[Item[IdState.Identified]]
                  )
              }
              .toList match {
              // We can return multiple items from multiple works
              // The sortBy above returns us the lowest bibId work association
              case List(item) => Right(item)
              case item :: _  => Right(item)
              case List()     => Left(DocumentNotFoundError(srcId))
            }

          case (Left(err), _) => Left(err)
        }
      }
  }
}

object ItemLookup {
  def apply(elasticClient: ElasticClient, index: Index)(
    implicit ec: ExecutionContext
  ): ItemLookup =
    new ItemLookup(
      elasticsearchService = new ElasticsearchService(elasticClient),
      index = index
    )
}
