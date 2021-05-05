package uk.ac.wellcome.platform.api.common.services

import uk.ac.wellcome.platform.api.common.models._
import uk.ac.wellcome.platform.api.common.services.source.CatalogueSource
import weco.catalogue.internal_model.identifiers.{
  CanonicalId,
  IdState,
  SourceIdentifier
}
import weco.catalogue.internal_model.identifiers.IdState.Identified
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraIdentifier
import weco.catalogue.internal_model.work.WorkState.Indexed
import weco.catalogue.internal_model.work.{Item, Work}

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

  def getSierraItemIdentifierNew(item: Item[IdState.Identified]): Option[SourceIdentifier] = {
    val identifiers = item.id.sourceIdentifier +: item.id.otherIdentifiers

    identifiers filter (_.identifierType == SierraIdentifier) match {
      case List(id) => Some(id)

      case Nil => None

      // This would be very unusual and probably points to a problem in the Catalogue API.
      // We throw a distinct error here so that it's easier to debug if this ever
      // occurs in practice.
      case multipleSierraIdentifiers =>
        throw new Exception(
          s"Multiple values for $SierraIdentifier: $multipleSierraIdentifiers"
        )
    }
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

  def getAllStacksItemsFromWorkNew(work: Work.Visible[Indexed]): List[StacksItemIdentifier] =
    work.data.items
      .collect {
        case item @ Item(Identified(canonicalId, _, _), _, _) =>
          (canonicalId, getSierraItemIdentifierNew(item.asInstanceOf[Item[IdState.Identified]]))
      }
      .collect {
        case (canonicalId, Some(sierraIdentifier)) =>
          StacksItemIdentifier(
            canonicalId = canonicalId,
            sierraId = SierraItemIdentifier(sierraIdentifier.value.toLong)
          )
      }

  def getAllStacksItemsFromWork(
    workId: CanonicalId
  ): Future[List[StacksItemIdentifier]] =
    for {
      workStub <- catalogueSource.getWorkStub(workId)
      items = getStacksItems(workStub.items)
    } yield items

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
