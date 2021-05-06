package uk.ac.wellcome.platform.api.common.services

import uk.ac.wellcome.platform.api.common.models._
import uk.ac.wellcome.platform.api.common.services.source.CatalogueSource
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class CatalogueService(
  val catalogueSource: CatalogueSource
)(implicit ec: ExecutionContext) {

  import CatalogueSource._

  private def getSierraItemIdentifier(
    identifiers: List[IdentifiersStub]
  ): Option[SierraItemIdentifier] =
    identifiers filter (_.identifierType.id == "sierra-identifier") match {
      case List(IdentifiersStub(_, value)) =>
        Try(value.toLong) match {
          case Success(l) => Some(SierraItemIdentifier(l))
          case Failure(_) =>
            throw new Exception(s"Unable to convert $value to Long!")
        }

      case Nil => None

      // This would be very unusual and probably points to a problem in the Catalogue API.
      // We throw a distinct error here so that it's easier to debug if this ever
      // occurs in practice.
      case multipleSierraIdentifiers =>
        throw new Exception(
          s"Multiple values for sierra-identifier: $multipleSierraIdentifiers"
        )
    }

  private def getStacksItems(
    itemStubs: List[ItemStub]
  ): List[StacksItemIdentifier] =
    itemStubs collect {
      case ItemStub(Some(id), Some(identifiers)) =>
        (
          CanonicalId(id),
          getSierraItemIdentifier(identifiers)
        )
    } collect {
      case (canonicalId, Some(sierraId)) =>
        StacksItemIdentifier(canonicalId, sierraId)
    }

  def getStacksItem(
    identifier: ItemIdentifier[_]
  ): Future[Option[StacksItemIdentifier]] =
    for {
      searchStub <- catalogueSource.getSearchStub(identifier)

      items = searchStub.results
        .map(_.items)
        .flatMap(getStacksItems)

      // Ensure we are only matching items that match the passed id!
      filteredItems = identifier match {
        case SierraItemIdentifier(id) =>
          items.filter(_.sierraId.value == id)
        case _ => items
      }

      // Items can appear on multiple works in a search result
      distinctFilteredItems = filteredItems.distinct

    } yield
      distinctFilteredItems match {
        case List(item) => Some(item)
        case Nil        => None
        case _ =>
          throw new Exception(
            s"Found multiple matching items for $identifier in: $distinctFilteredItems"
          )
      }

  def getStacksItemFromItemId(
    itemId: CanonicalId
  ): Future[Option[StacksItemIdentifier]] =
    for {
      searchStub <- catalogueSource.getSearchStub(itemId)

      items = searchStub.results
        .map(_.items)
        .flatMap(getStacksItems)

      // Ensure we are only matching items that match the passed id!
      filteredItems = items.filter(_.canonicalId == itemId)

      // Items can appear on multiple works in a search result
      distinctFilteredItems = filteredItems.distinct

    } yield
      distinctFilteredItems match {
        case List(item) => Some(item)
        case Nil        => None
        case _ =>
          throw new Exception(
            s"Found multiple matching items for $itemId in: $distinctFilteredItems"
          )
      }
}
