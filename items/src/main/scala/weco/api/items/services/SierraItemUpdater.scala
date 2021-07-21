package weco.api.items.services

import grizzled.slf4j.Logging
import weco.api.stacks.models.SierraItemIdentifier
import weco.api.stacks.services.SierraService
import weco.catalogue.internal_model.identifiers.{IdState, IdentifierType}
import weco.catalogue.internal_model.locations.{AccessCondition, PhysicalLocation}
import weco.catalogue.internal_model.work.Item
import weco.catalogue.source_model.sierra.identifiers.SierraItemNumber

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class SierraItemUpdater(sierraService: SierraService)(implicit executionContext: ExecutionContext)
  extends ItemUpdater
    with Logging {

  val identifierType = IdentifierType.SierraSystemNumber

  private def updateLocations(
                               item: Item[IdState.Minted],
                               accessCondition: AccessCondition
                             ) = item.locations.map {
    case physicalLocation: PhysicalLocation =>
      physicalLocation.copy(
        accessConditions = List(accessCondition)
      )
    case location => location
  }

  private def updateAccessConditions(
                                      sierraItemSourceIdentifiers: Map[SierraItemNumber, Item[IdState.Minted]],
                                      accessConditions: Map[SierraItemNumber, Option[AccessCondition]]
                                    ): immutable.Iterable[Item[IdState.Minted]] = {
    accessConditions.flatMap {
      case (itemNumber, Some(accessCondition)) => {
        sierraItemSourceIdentifiers
          .get(itemNumber)
          .map(item => {
            item.copy(
              locations = updateLocations(item, accessCondition)
            )
          })
      }
    }
  }

  def updateItems(items: Seq[Item[IdState.Minted]]): Future[Seq[Item[IdState.Minted]]] = {
    val sierraItemSourceIdentifiers = items.map {
      case item@Item(IdState.Identified(_, srcId, _), _, _, _) =>
        SierraItemIdentifier.fromSourceIdentifier(srcId) -> item
    } toMap

    val itemNumbers = sierraItemSourceIdentifiers.keys.toSeq

    sierraService.getAccessConditions(itemNumbers)
      .map {
        case Right(accessConditions) =>
          updateAccessConditions(sierraItemSourceIdentifiers, accessConditions)
        case Left(err) =>
          error(msg = f"Couldn't refresh items for $itemNumbers got error $err")
          items
      } map(_.toSeq)
  }
}
