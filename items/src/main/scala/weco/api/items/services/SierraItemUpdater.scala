package weco.api.items.services

import grizzled.slf4j.Logging
import weco.sierra.http.SierraSource
import weco.api.stacks.models.SierraItemIdentifier
import weco.catalogue.display_model.locations.{
  DisplayAccessCondition,
  DisplayPhysicalLocation
}
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
    with Logging {

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
    itemNumbers: Seq[SierraItemNumber]
  ): Future[Map[SierraItemNumber, AccessCondition]] =
    for {
      itemEither <- sierraSource.lookupItemEntries(itemNumbers)

      accessConditions = itemEither match {
        case Right(SierraItemDataEntries(_, _, entries)) =>
          entries.map(item => item.id -> item.accessCondition).toMap
        case Left(
            SierraItemLookupError.MissingItems(missingItems, itemsReturned)
            ) =>
          warn(s"Item lookup missing items: $missingItems")
          itemsReturned.map(item => item.id -> item.accessCondition).toMap
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
      .filter { case (_, item) => isStale(item) }

    debug(
      s"Asked to update items ${itemMap.keySet}, refreshing stale items ${staleItems.keySet}"
    )

    for {
      accessConditions <- getUpdatedAccessConditions(staleItems.keys.toSeq)

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
    itemIds: Seq[SierraItemNumber]
  ): Future[Map[SierraItemNumber, AccessCondition]] =
    itemIds match {
      case Nil => Future.successful(Map())

      case _ =>
        for {
          accessConditionsMap <- getAccessConditions(itemIds)

          // It is possible for there to be a situation where Sierra does not know about
          // an Item that is in the Catalogue API, but this situation should be very rare.
          // For example an item has been deleted but the change has not yet propagated.
          // In that case it gets method "NotRequestable".
          missingItemIds = itemIds
            .filterNot { accessConditionsMap.keySet.contains(_) }

          missingItemsMap = missingItemIds
            .map(_ -> AccessCondition(method = AccessMethod.NotRequestable))
            .toMap
        } yield accessConditionsMap ++ missingItemsMap
    }

  /** There are two cases we care about where the data in the catalogue API
    * might be stale:
    *
    *   1) An item was on request for a user, but has been returned to the stores.
    *      Another user could now request the item, but the catalogue API will
    *      tell you it's temporarily unavailable.
    *
    *   2) An item is in the closed stores, and been requested by a user.
    *      Another user can no longer request the item, but the catalogue API will
    *      tell you it's available.
    *
    * In all other cases, we can use the access conditions in the catalogue API.
    * There may be a small delay in updating an item's information, but this is
    * relatively tolerable, and allows us to simplify the logic in this API for
    * determining item status.
    *
    */
  private def isStale(item: DisplayItem): Boolean = {

    // In practice we know an item only has one access condition
    val accessCondition = item.locations
      .collect { case loc: DisplayPhysicalLocation => loc }
      .flatMap(_.accessConditions)
      .headOption

    val statusId = accessCondition
      .flatMap(_.status)
      .map(_.id)

    val methodId = accessCondition.map(_.method.id)

    val isTemporarilyUnavailable = statusId.contains("temporarily-unavailable")

    val isOnlineRequest = methodId.contains("online-request")
    val hasRequestableStatus = statusId.contains("open") || statusId.contains(
      "open-with-advisory"
    ) || statusId.contains("restricted") || statusId.isEmpty

    val isRequestable = isOnlineRequest && hasRequestableStatus

    isTemporarilyUnavailable || isRequestable
  }
}
