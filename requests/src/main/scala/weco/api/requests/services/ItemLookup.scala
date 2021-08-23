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

  /** Look up a collection of items and the corresponding Work data.
    *
    * At least within Sierra, i's possible for a single Item to be associated with
    * multiple Works, e.g. if multiple items are bound/contained together.
    * For an extreme example, see Item i13000780 / ty6qpt7d, which is on 705 Works.
    *
    * We want to return a consistent title/work ID to the user in the list of holds,
    * so we use the work with the lowest alphabetical source identifier (i.e. lowest bib number).
    * This mirrors what Encore/OPAC seems to do -- if an item is on multiple bibs,
    * the list of user holds links to the lowest numbered bib.
    *
    * We might want to remember the original request, and which Work the user was looking
    * at, but that's a bigger piece of work.  It involves UX input on how to best explain
    * the same item on multiple works.  Making this change is tracked in a separate ticket.
    * See https://github.com/wellcomecollection/platform/issues/5267
    *
    */
  def bySourceIdentifier(
    itemIdentifiers: Seq[SourceIdentifier]
  ): Future[Seq[Either[ElasticsearchError, RequestedItemWithWork]]] = {
    val multiSearchRequest = MultiSearchRequest(
      itemIdentifiers.map { itemSourceIdentifier =>
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
                  value = itemSourceIdentifier.value
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
      .findByMultiSearch[Work.Visible[Indexed]](multiSearchRequest)
      .map {
        _.zip(itemIdentifiers).map {
          case (Right(works), srcId) => addWorkDataToItem(works, srcId)
          case (Left(err), _)        => Left(err)
        }
      }
  }

  private def addWorkDataToItem(works: Seq[Work.Visible[Indexed]], itemIdentifier: SourceIdentifier): Either[DocumentNotFoundError[SourceIdentifier], RequestedItemWithWork] =
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
          ) if id.sourceIdentifier == itemIdentifier =>
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
        case Nil   => Left(DocumentNotFoundError(itemIdentifier))
        case items => Right(items.head)
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
