package weco.api.requests.services

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import com.sksamuel.elastic4s.Index
import grizzled.slf4j.Logging
import weco.api.requests.models.RequestedItemWithWork
import weco.api.search.elasticsearch.ElasticsearchService
import weco.catalogue.display_model.models.{DisplayIdentifier, DisplayItem, DisplayWork}
import weco.catalogue.internal_model.identifiers.{CanonicalId, IdState, SourceIdentifier}
import weco.catalogue.internal_model.work.Item
import weco.http.client.{HttpClient, HttpGet}
import weco.http.json.CirceMarshalling
import weco.json.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

sealed trait ItemLookupError {
  val err: Throwable
}

case class ItemNotFoundError(id: Any, err: Throwable) extends ItemLookupError
case class UnknownItemError(id: Any, err: Throwable)
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
  ): Future[Seq[Either[ItemLookupError, RequestedItemWithWork]]] =
    itemIdentifiers match {
      // If there are no identifiers, return the result immediately.  This is quite
      // common in practice (new users, or users who haven't ordered items recently).
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
  ): Future[Seq[Either[ItemLookupError, RequestedItemWithWork]]] = {
    require(itemIdentifiers.nonEmpty)

    val path = Path("works")
    val params = Map(
      "include" -> "identifiers,items",
      "identifiers" -> itemIdentifiers.map(_.value).mkString(","),
      "pageSize" -> "100"
    )

    val httpResult = for {
      response <- client.get(path = path, params = params)

      result <- response.status match {
        case StatusCodes.OK =>
          info(s"OK for GET to $path with $params")
          Unmarshal(response.entity).to[DisplayWorkResults].map { results =>
            // Sort by source identifier value
            val works = results.results.sortBy(_.identifiers.get.head.value)

            val items: Seq[(DisplayWork, DisplayItem)] = works
              .flatMap { w =>
                w.items.getOrElse(List()).map(item => (w, item))
              }

            itemIdentifiers.map { itemId =>
              val matchingWorks = items.flatMap { case (work, item) =>
                val identifiers = item.identifiers.getOrElse(List())

                val isMatchingItem = identifiers.exists(id =>
                  id.value == itemId.value && id.identifierType.id == itemId.identifierType.id
                )

                if (isMatchingItem) Some((work, item)) else None
              }

              matchingWorks.headOption match {
                case Some((work, item)) =>
                  Right(
                    RequestedItemWithWork(
                      workId = CanonicalId(work.id),
                      workTitle = work.title,
                      item = item.asInstanceOf[Item[IdState.Identified]]
                    )
                  )

                case None =>
                  Left(ItemNotFoundError(itemId.value, err = new Throwable(s"Could not find item $itemId")))
              }
            }
          }

        case status =>
          val err = new Throwable(s"$status from the catalogue API")
          error(
            s"Unexpected status from GET to $path with $params: $status",
            err
          )
          Future(itemIdentifiers.map(id => Left(UnknownItemError(id, err))))
      }
    } yield result

    httpResult.recover { case e => itemIdentifiers.map(id => Left(UnknownItemError(id, e))) }
  }
}
