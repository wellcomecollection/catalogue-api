package weco.api.items.services

import grizzled.slf4j.Logging
import weco.sierra.http.SierraSource
import weco.api.stacks.models.SierraItemIdentifier
import weco.catalogue.internal_model.identifiers.{IdState, IdentifierType}
import weco.catalogue.internal_model.locations.{
  AccessCondition,
  AccessMethod,
  PhysicalLocation
}
import weco.catalogue.internal_model.work.Item
import weco.catalogue.source_model.sierra.rules.{
  SierraItemAccess,
  SierraPhysicalLocationType
}
import weco.sierra.models.data.SierraItemData
import weco.sierra.models.errors.SierraItemLookupError
import weco.sierra.models.fields.SierraItemDataEntries
import weco.sierra.models.identifiers.{SierraBibNumber, SierraItemNumber}

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
    item: Item[IdState.Identified],
    accessCondition: AccessCondition
  ): Item[IdState.Identified] = {
    val updatedItemLocations = item.locations.map {
      case physicalLocation: PhysicalLocation =>
        physicalLocation.copy(
          accessConditions = List(accessCondition)
        )
      case location => location
    }

    item.copy(locations = updatedItemLocations)
  }

  private def updateAccessConditions(
    itemMap: Map[SierraItemNumber, Item[IdState.Identified]],
    accessConditionMap: Map[SierraItemNumber, AccessCondition]
  ): Seq[Item[IdState.Identified]] =
    itemMap.map {
      case (itemNumber, item) =>
        accessConditionMap
          .get(itemNumber)
          .map(updateAccessCondition(item, _))
          .getOrElse(item)
    } toSeq

  private def getAccessCondition(itemData: SierraItemData): AccessCondition = {
    // The bib ID is used for debugging purposes; the bib status is only used
    // for consistency checking. We can use placeholder data here.
    val (accessCondition, _) = SierraItemAccess(
      bibId = SierraBibNumber("0000000"),
      bibStatus = None,
      location = itemData.fixedFields
        .get("79")
        .flatMap(_.display)
        .flatMap(
          name => SierraPhysicalLocationType.fromName(itemData.id, name)
        ),
      itemData = itemData
    )

    accessCondition
  }

  def getAccessConditions(
    itemNumbers: Seq[SierraItemNumber]
  ): Future[Map[SierraItemNumber, AccessCondition]] =
    for {
      itemEither <- sierraSource.lookupItemEntries(itemNumbers)

      accessConditions = itemEither match {
        case Right(SierraItemDataEntries(_, _, entries)) =>
          entries.map(item => item.id -> getAccessCondition(item)).toMap
        case Left(
            SierraItemLookupError.MissingItems(missingItems, itemsReturned)
            ) =>
          warn(s"Item lookup missing items: $missingItems")
          itemsReturned.map(item => item.id -> getAccessCondition(item)).toMap
        case Left(itemLookupError) =>
          error(s"Item lookup failed: $itemLookupError")
          Map.empty[SierraItemNumber, AccessCondition]
      }
    } yield accessConditions

  def updateItems(
    items: Seq[Item[IdState.Identified]]
  ): Future[Seq[Item[IdState.Identified]]] = {
    val itemMap = items.map {
      case item @ Item(IdState.Identified(_, srcId, _), _, _, _) =>
        SierraItemIdentifier.fromSourceIdentifier(srcId) -> item
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
