package weco.api.requests.services

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import com.sksamuel.elastic4s.ElasticApi.fieldSort
import com.sksamuel.elastic4s.ElasticDsl.{boolQuery, search, termQuery}
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.requests.searches.MultiSearchRequest
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import grizzled.slf4j.Logging
import weco.api.requests.models.RequestedItemWithWork
import weco.api.search.elasticsearch.{DocumentNotFoundError, ElasticsearchError, ElasticsearchService}
import weco.catalogue.display_model.models.{DisplayIdentifier, DisplayWork}
import weco.catalogue.internal_model.identifiers.{CanonicalId, IdState, SourceIdentifier}
import weco.catalogue.internal_model.work.Item
import weco.http.client.{HttpClient, HttpGet}
import weco.http.json.CirceMarshalling
import weco.json.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

sealed trait ItemLookupError {
  val err: Throwable
}

case class ItemNotFoundError(id: CanonicalId, err: Throwable) extends ItemLookupError
case class UnknownItemError(id: CanonicalId, err: Throwable)
  extends ItemLookupError

case class DisplayWorkResults(
  results: Seq[DisplayWork]
)

class ItemLookup(
  client: HttpClient with HttpGet,
  elasticsearchService: ElasticsearchService,
  index: Index
)(
  implicit
  as: ActorSystem,
  ec: ExecutionContext
) extends Logging {

  import weco.catalogue.display_model.models.Implicits._

  implicit val um: Unmarshaller[HttpEntity, DisplayWorkResults] =
    CirceMarshalling.fromDecoder[DisplayWorkResults]

  /** Returns the SourceIdentifier of the item that corresponds to this
    * canonical ID.
    *
    */
  def byCanonicalId(
    itemId: CanonicalId
  ): Future[Either[ItemLookupError, DisplayIdentifier]] = {
    val path = Path("works")
    val params = Map("include" -> "identifiers,items", "identifiers" -> itemId.underlying, "pageSize" -> "1")

    val httpResult = for {
      response <- client.get(path = path, params = params)

      result <- response.status match {
        case StatusCodes.OK =>
          info(s"OK for GET to $path with $params")
          Unmarshal(response.entity).to[DisplayWorkResults].map { results =>
            val items = results.results.flatMap(_.items).flatten

            items.find(_.id.contains(itemId.underlying)).flatMap(item => item.identifiers.getOrElse(List()).headOption) match {
              case Some(identifier) => Right(identifier)
              case None             => Left(ItemNotFoundError(itemId, err = new Throwable(s"Could not find item $itemId")))
            }
          }

        case status =>
          val err = new Throwable(s"$status from the catalogue API")
          error(
            s"Unexpected status from GET to $path with $params: $status",
            err
          )
          Future(Left(UnknownItemError(itemId, err)))
      }
    } yield result

    httpResult.recover { case e => Left(UnknownItemError(itemId, e)) }
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

      case _ => searchBySourceIdentifier(itemIdentifiers)
    }

  private case class WorkStubData(
    title: Option[String],
    items: List[Item[IdState.Minted]]
  )

  private case class WorkStubState(canonicalId: CanonicalId)

  private case class WorkStub(data: WorkStubData, state: WorkStubState)

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
          .sourceInclude("data.items", "data.title", "state.canonicalId")
          .sortBy(
            fieldSort("state.sourceIdentifier.value")
              .order(SortOrder.Asc)
          )
          // We are sorting so we're only interested in the top value
          .size(1)
      }
    )

    elasticsearchService
      .findByMultiSearch[WorkStub](multiSearchRequest)
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
    work: WorkStub,
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

  private implicit class WorkOps(w: WorkStub) {

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
  }
}
