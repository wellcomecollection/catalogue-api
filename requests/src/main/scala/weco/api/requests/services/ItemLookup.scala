package weco.api.requests.services

import com.sksamuel.elastic4s.ElasticApi.fieldSort
import com.sksamuel.elastic4s.ElasticDsl.{boolQuery, search, termQuery}
import com.sksamuel.elastic4s.requests.searches.MultiSearchRequest
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import com.sksamuel.elastic4s.{ElasticClient, Index}
import grizzled.slf4j.Logging
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
) extends Logging {

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

    elasticsearchService
      .findBySearch[Work.Visible[Indexed]](searchRequest)
      .map {
        case Left(err) => Left(err)
        case Right(works) =>
          val item = works
            .flatMap(w => w.itemWith(itemId))
            .headOption

          item match {
            case Some(it) => Right(it)
            case None     => Left(DocumentNotFoundError(itemId))
          }
      }
  }

  /** Look up a collection of items and the corresponding Work data.
    *
    * At least within Sierra, it's possible for a single Item to be associated with
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
  ): Future[Seq[Either[ElasticsearchError, RequestedItemWithWork]]] =
    itemIdentifiers match {
      // If there are no identifiers, return the result immediately.  This will be
      // reasonably common in practice (new users, or users who haven't ordered items
      // recently), and if you actually try to search an empty list of identifiers in
      // Elasticsearch you get a warning:
      //
      //      support for empty first line before any action metadata in msearch API
      //      is deprecated and will be removed in the next major version
      //
      case Nil => Future.successful(Seq())

      case _   => searchBySourceIdentifier(itemIdentifiers)
    }

  private def searchBySourceIdentifier(
    itemIdentifiers: Seq[SourceIdentifier]
  ): Future[Seq[Either[ElasticsearchError, RequestedItemWithWork]]] = {
    require(itemIdentifiers.nonEmpty)

    val multiSearchRequest = MultiSearchRequest(
      itemIdentifiers.map { itemSourceIdentifier =>
        search(index)
          .query(
            boolQuery
              .filter(
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
          case (Right(Seq(work)), itemId) => addWorkDataToItem(work, itemId)
          case (Right(Nil), itemId) =>
            warn(s"No works found matching item identifier $itemId")
            Left(DocumentNotFoundError(itemId))

          // This should never happen in practice, because we have .size(1) in the query.
          // We can still return something to the user here, but log a warning so we
          // know something's gone wrong.
          case (Right(works), itemId) =>
            warn(
              s"Multiple works (${works.size}) found matching item identifier $itemId"
            )
            addWorkDataToItem(works.head, itemId)

          case (Left(err), _) => Left(err)
        }
      }
  }

  private def addWorkDataToItem(
    work: Work.Visible[Indexed],
    itemIdentifier: SourceIdentifier
  ): Either[DocumentNotFoundError[SourceIdentifier], RequestedItemWithWork] =
    work.itemWith(itemIdentifier) match {
      case Some(item) =>
        Right(
          RequestedItemWithWork(
            workId = work.state.canonicalId,
            workTitle = work.data.title,
            item = item.asInstanceOf[Item[IdState.Identified]]
          )
        )

      case None => Left(DocumentNotFoundError(itemIdentifier))
    }

  private implicit class WorkOps(w: Work.Visible[Indexed]) {

    // You might expect you can write these functions as something like:
    //
    //      case item: Item[IdState.Identified] if item.id.canonicalId == itemId =>
    //
    // but if you do, you get an error from the compiler:
    //
    //      non-variable type argument IdState.Identified in type pattern Item[IdState.Identified]
    //      is unchecked since it is eliminated by erasure
    //
    // The use of .asInstanceOf is to keep the compiler happy.

    def itemWith(
      itemSourceId: SourceIdentifier
    ): Option[Item[IdState.Identified]] =
      w.data.items.collectFirst {
        case item @ Item(id @ IdState.Identified(_, _, _), _, _, _)
            if id.sourceIdentifier == itemSourceId =>
          item.asInstanceOf[Item[IdState.Identified]]
      }

    def itemWith(itemId: CanonicalId): Option[Item[IdState.Identified]] =
      w.data.items.collectFirst {
        case item @ Item(id @ IdState.Identified(_, _, _), _, _, _)
            if id.canonicalId == itemId =>
          item.asInstanceOf[Item[IdState.Identified]]
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
