package weco.api.items.services

import grizzled.slf4j.Logging
import weco.api.stacks.models.SierraItemIdentifier
import weco.api.stacks.services.SierraService
import weco.catalogue.internal_model.identifiers.{IdState, IdentifierType}
import weco.catalogue.internal_model.locations.{
  AccessCondition,
  PhysicalLocation
}
import weco.catalogue.internal_model.work.Item
import weco.catalogue.source_model.sierra.identifiers.SierraItemNumber

import scala.concurrent.{ExecutionContext, Future}

class SierraItemUpdater(sierraService: SierraService)(
  implicit executionContext: ExecutionContext
) extends ItemUpdater
    with Logging {

  val identifierType = IdentifierType.SierraSystemNumber

  private def updateAccessCondition(
    item: Item[IdState.Identified],
    accessCondition: AccessCondition
  ) = {
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
  ) =
    itemMap.map {
      case (itemNumber, item) =>
        accessConditionMap
          .get(itemNumber)
          .map(updateAccessCondition(item, _))
          .getOrElse(item)
    } toSeq

  def updateItems(
    items: Seq[Item[IdState.Identified]]
  ): Future[Seq[Item[IdState.Identified]]] = {
    val itemMap = items.map {
      case item @ Item(IdState.Identified(_, srcId, _), _, _, _) =>
        SierraItemIdentifier.fromSourceIdentifier(srcId) -> item
    } toMap

    sierraService
      .getAccessConditions(itemMap.keys.toSeq)
      .map(
        updateAccessConditions(itemMap, _)
      )
  }
}
