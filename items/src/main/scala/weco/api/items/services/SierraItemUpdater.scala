package weco.api.items.services

import grizzled.slf4j.Logging
import weco.sierra.http.SierraSource
import weco.api.stacks.models.{DisplayItemOps, SierraItemIdentifier}
import weco.catalogue.display_model.locations.{DisplayAccessCondition, DisplayLocationType, DisplayPhysicalLocation}
import weco.catalogue.display_model.work.DisplayItem
import weco.catalogue.internal_model.identifiers.IdentifierType
import weco.catalogue.internal_model.locations.{AccessCondition, AccessMethod}
import weco.sierra.models.errors.SierraItemLookupError
import weco.sierra.models.fields.SierraItemDataEntries
import weco.sierra.models.identifiers.SierraItemNumber

import scala.concurrent.{ExecutionContext, Future}

/** Updates the AccessCondition of sierra items
  *
  *  This provides an up to date view on whether a hold
  *  can be placed on an item.
  *
  */
class SierraItemUpdater(sierraSource: SierraSource)(
  implicit executionContext: ExecutionContext
) extends ItemUpdater
    with Logging
    with DisplayItemOps {

  import weco.api.stacks.models.SierraItemDataOps._

  val identifierType = IdentifierType.SierraSystemNumber

  /** Updates the AccessCondition for a single item
    *
    *  We are interested in updating the status of an Item
    *  a library patron can request. These are items with a
    *  PhysicalLocation. In data sourced from Sierra we can
    *  only have one PhysicalLocation, so we update it if
    *  we find it.
    *
    */
  private def updateAccessCondition(
    item: DisplayItem,
    accessCondition: AccessCondition
  ): DisplayItem = {
    val updatedItemLocations = item.locations.map {
      case physicalLocation: DisplayPhysicalLocation =>
        physicalLocation.copy(
          accessConditions = List(DisplayAccessCondition(accessCondition))
        )
      case location => location
    }

    item.copy(locations = updatedItemLocations)
  }

  private def getAccessConditions(
                                   existingItems: Map[SierraItemNumber, Option[DisplayLocationType]]
  ): Future[Map[SierraItemNumber, AccessCondition]] =
    for {
      itemEither <- sierraSource.lookupItemEntries(existingItems.keys.toSeq)

      accessConditions = itemEither match {
        case Right(SierraItemDataEntries(_, _, entries)) =>
          entries
            .map(item => {
              val location = existingItems.get(item.id).flatten
              item.id -> item.accessCondition(location)
            })
            .toMap
        case Left(
            SierraItemLookupError.MissingItems(missingItems, itemsReturned)
            ) =>
          warn(s"Item lookup missing items: $missingItems")
          itemsReturned
            .map(item => {
              val location = existingItems.get(item.id).flatten
              item.id -> item.accessCondition(location)
            })
            .toMap
        case Left(itemLookupError) =>
          error(s"Item lookup failed: $itemLookupError")
          Map.empty[SierraItemNumber, AccessCondition]
      }
    } yield accessConditions

  def updateItems(items: Seq[DisplayItem]): Future[Seq[DisplayItem]] = {

    // item number -> item
    val itemMap = items.map { item =>
      SierraItemIdentifier.fromSourceIdentifier(item.identifiers.head) -> item
    } toMap

    val staleItems = itemMap
      .filter { case (_, item) => item.isStale }
      .map { case (itemId, item) => itemId -> item.physicalLocationType }

    debug(
      s"Asked to update items ${itemMap.keySet}, refreshing stale items ${staleItems.keySet}"
    )

    for {
      accessConditions <- getUpdatedAccessConditions(staleItems)

      updatedItems = itemMap.map {
        case (sierraId, item) =>
          accessConditions.get(sierraId) match {
            case Some(updatedAc) => updateAccessCondition(item, updatedAc)
            case None            => item
          }
      }
    } yield updatedItems.toSeq
  }

  /** Given a series of item IDs, get the most up-to-date access condition
    * information from Sierra.
    *
    */
  private def getUpdatedAccessConditions(
    itemIds: Map[SierraItemNumber, Option[DisplayLocationType]]
  ): Future[Map[SierraItemNumber, AccessCondition]] =
    itemIds.size match {
      case 0 => Future.successful(Map())

      case _ =>
        for {
          accessConditionsMap <- getAccessConditions(itemIds)

          // It is possible for there to be a situation where Sierra does not know about
          // an Item that is in the Catalogue API, but this situation should be very rare.
          // For example an item has been deleted but the change has not yet propagated.
          // In that case it gets method "NotRequestable".
          missingItemIds = itemIds.keySet.diff(accessConditionsMap.keySet)

          missingItemsMap = missingItemIds
            .map(_ -> AccessCondition(method = AccessMethod.NotRequestable))
            .toMap
        } yield accessConditionsMap ++ missingItemsMap
    }
}
