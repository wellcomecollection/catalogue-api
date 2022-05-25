package weco.api.requests.services

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import grizzled.slf4j.Logging
import weco.api.requests.models.RequestedItemWithWork
import weco.api.stacks.models.{CatalogueWork, DisplayItemOps}
import weco.catalogue.display_model.Implicits._
import weco.catalogue.display_model.identifiers.DisplayIdentifier
import weco.catalogue.display_model.work.DisplayItem
import weco.http.client.{HttpClient, HttpGet}
import weco.http.json.CirceMarshalling
import weco.json.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

sealed trait ItemLookupError {
  val err: Throwable
}

case class ItemNotFoundError(id: Any, err: Throwable) extends ItemLookupError
case class UnknownItemError(id: Any, err: Throwable) extends ItemLookupError

case class CatalogueWorkResults(
  totalResults: Int,
  results: Seq[CatalogueWork]
)

class ItemLookup(client: HttpClient with HttpGet)(
  implicit
  as: ActorSystem,
  ec: ExecutionContext
) extends Logging
    with DisplayItemOps {

  implicit val um: Unmarshaller[HttpEntity, CatalogueWorkResults] =
    CirceMarshalling.fromDecoder[CatalogueWorkResults]

  /** Returns the item for this canonical ID. */
  def byCanonicalId(
    itemId: String
  ): Future[Either[ItemLookupError, DisplayItem]] = {
    val path = Path("works")
    val params = Map(
      "include" -> "identifiers,items",
      "items" -> itemId,
      "pageSize" -> "1"
    )

    val httpResult = for {
      response <- client.get(path = path, params = params)

      result <- response.status match {
        case StatusCodes.OK =>
          info(s"OK for GET to $path with $params")
          Unmarshal(response.entity).to[CatalogueWorkResults].map { results =>
            val items = results.results.flatMap(_.items)

            val matchingItem = items.find(_.id.contains(itemId))

            matchingItem match {
              case Some(item) => Right(item)
              case None =>
                Left(
                  ItemNotFoundError(
                    itemId,
                    err = new Throwable(s"Could not find item $itemId")
                  )
                )
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
    itemIdentifiers: Seq[DisplayIdentifier]
  ): Future[Seq[Either[ItemLookupError, RequestedItemWithWork]]] =
    itemIdentifiers match {
      // If there are no identifiers, return the result immediately.  This is quite
      // common in practice (new users, or users who haven't ordered items recently).
      case Nil => Future.successful(Seq())

      case _ => searchBySourceIdentifier(itemIdentifiers)
    }

  private def searchBySourceIdentifier(
    itemIdentifiers: Seq[DisplayIdentifier]
  ): Future[Seq[Either[ItemLookupError, RequestedItemWithWork]]] = {
    require(itemIdentifiers.nonEmpty)
    val pageSize = 100

    def getWorksPage(
      itemIds: Seq[DisplayIdentifier]
    ): Future[CatalogueWorkResults] = {
      val path = Path("works")
      val params = Map(
        "include" -> "identifiers,items",
        "items.identifiers" -> itemIds.map(_.value).mkString(","),
        "pageSize" -> pageSize.toString
      )
      client.get(path, params).flatMap { response =>
        response.status match {
          case StatusCodes.OK =>
            info(s"OK for GET to $path with $params")
            Unmarshal(response.entity).to[CatalogueWorkResults]
          case errorStatus =>
            val err = new Throwable(s"$errorStatus from the catalogue API")
            error(
              s"Unexpected status from GET to $path with $params: $errorStatus",
              err
            )
            Future.failed(err)
        }
      }
    }

    getWorksPage(itemIdentifiers)
      .flatMap { worksPage =>
        val works =
          worksPage.results.sortBy(_.identifiers.headOption.map(_.value))
        getRequestedItemsFromWorks(itemIdentifiers, works) match {
          // If we don't have any more results, or if we've only looked for one ID,
          // we know that these works are all available for the requested item identifiers.
          // That means that any missing items here are not available in the catalogue API.
          case RequestedItemsFromWorks(found, notFound)
              if worksPage.totalResults <= pageSize || itemIdentifiers.size == 1 =>
            Future.successful(
              found.map(Right(_)) ++ notFound.map(
                itemId =>
                  Left(
                    ItemNotFoundError(
                      itemId.value,
                      err = new Throwable(s"Could not find item $itemId")
                    )
                  )
              )
            )
          // The alternative case is that there are more results available (ie they span more than 1 page).
          // In this case, we make a single search for each item, thereby guaranteeing that
          // we'll find a work with that item if it exists in the catalogue API. Pagination is unnecessary
          // because a search for a single item ID will necessarily only return works containing the corresponding item.
          case RequestedItemsFromWorks(foundSubset, notYetFound) =>
            val individualItemRequests = notYetFound.map(
              itemId => searchBySourceIdentifier(Seq(itemId))
            )
            Future
              .sequence(individualItemRequests)
              .map(results => foundSubset.map(Right(_)) ++ results.flatten)
        }
      }
      .recover {
        case e => itemIdentifiers.map(id => Left(UnknownItemError(id, e)))
      }
  }

  private case class RequestedItemsFromWorks(
    found: Seq[RequestedItemWithWork],
    notFound: Seq[DisplayIdentifier]
  )
  private def getRequestedItemsFromWorks(
    itemIdentifiers: Seq[DisplayIdentifier],
    works: Seq[CatalogueWork]
  ): RequestedItemsFromWorks = {
    val matchingItems = for {
      work <- works
      itemId <- itemIdentifiers
      item <- work.getItem(itemId)
    } yield (item, work)
    val notFound = itemIdentifiers.filterNot(
      itemId =>
        matchingItems.exists(_._1.identifiers.headOption.contains(itemId))
    )
    RequestedItemsFromWorks(
      found = matchingItems
      // If the item is on multiple works, choose the first one
        .groupBy(_._1.identifiers.headOption)
        .flatMap(_._2.headOption)
        .map {
          case (item, work) => RequestedItemWithWork(item, work)
        }
        .toSeq,
      notFound
    )
  }

  implicit class WorkOps(work: CatalogueWork) {
    def getItem(itemIdentifier: DisplayIdentifier): Option[DisplayItem] =
      work.items.find(_.identifiers.headOption.contains(itemIdentifier))
  }
}
