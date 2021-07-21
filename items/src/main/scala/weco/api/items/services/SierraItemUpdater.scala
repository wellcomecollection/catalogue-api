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

  // TODO: Can we cover multiple types here?
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
                                      itemNumberMap: Map[SierraItemNumber, Item[IdState.Minted]],
                                      accessConditions: Map[SierraItemNumber, AccessCondition]
  ): immutable.Iterable[Item[IdState.Minted]] = {
    require(itemNumberMap.keySet == accessConditions.keySet,
      s"Inconsistent update to AccessConditions set! Original: $itemNumberMap, updated: $accessConditions"
    )

    //todo: Can I just sort both maps on keys, then zip them?

    accessConditions.flatMap {
      case (itemNumber, accessCondition) => {
        itemNumberMap
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
