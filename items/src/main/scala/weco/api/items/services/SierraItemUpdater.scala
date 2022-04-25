package weco.api.items.services

import grizzled.slf4j.Logging
import weco.sierra.http.SierraSource
import weco.api.stacks.models.SierraItemIdentifier
import weco.catalogue.display_model.locations.DisplayPhysicalLocation
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

  private def updateAccessConditions(
    itemMap: Map[SierraItemNumber, DisplayItem],
    accessConditionMap: Map[SierraItemNumber, AccessCondition]
  ): Seq[DisplayItem] =
    itemMap.map {
      case (itemNumber, item) =>
        accessConditionMap
          .get(itemNumber)
          .map(updateAccessCondition(item, _))
          .getOrElse(item)
    } toSeq

  def getAccessConditions(
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
    val itemMap = items.map { item =>
      SierraItemIdentifier.fromSourceIdentifier(item.identifiers.head) -> item
    } toMap

    val accessConditions = for {
      accessConditionsMap <- getAccessConditions(itemMap.keys.toSeq)

      // It is possible for there to be a situation where Sierra does not know about
      // an Item that is in the Catalogue API, but this situation should be very rare.
      // For example an item has been deleted but the change has not yet propagated.
      // In that case it gets method "NotRequestable".
      missingItemsKeys = itemMap.filterNot {
        case (sierraItemNumber, _) =>
          accessConditionsMap.keySet.contains(sierraItemNumber)
      } keySet

      missingItemsMap = missingItemsKeys
        .map(_ -> AccessCondition(method = AccessMethod.NotRequestable)) toMap
    } yield accessConditionsMap ++ missingItemsMap

    accessConditions.map(updateAccessConditions(itemMap, _))
  }
}
