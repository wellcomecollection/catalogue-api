package weco.api.items.services

import grizzled.slf4j.Logging
import weco.api.stacks.models.SierraItemIdentifier
import weco.api.stacks.services.SierraService
import weco.catalogue.internal_model.identifiers.{IdState, IdentifierType, SourceIdentifier}
import weco.catalogue.internal_model.locations.{AccessCondition, PhysicalLocation}
import weco.catalogue.internal_model.work.{Item, Work, WorkState}
import weco.catalogue.source_model.sierra.identifiers.SierraItemNumber

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class ItemUpdateService(
  sierraService: SierraService
)(implicit executionContext: ExecutionContext)
    extends Logging {

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

  def updateSierraItem(items: Seq[Item[IdState.Minted]]) = {
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

  private def buildItemsMap(items: Seq[Item[IdState.Minted]]): Map[SourceIdentifier, Item[IdState.Minted]] =
    items.map {
      case item@Item(IdState.Identified(_, srcId, _), _, _, _) => srcId -> item
    } toMap

  private def preserveOrder(itemsWithIndex: Seq[(Item[IdState.Minted], Int)], f: Seq[Item[IdState.Minted]] => Future[Seq[Item[IdState.Minted]]]) = {
    val itemsMap = buildItemsMap(itemsWithIndex.map(_._1))
    val indexes = itemsWithIndex.map(_._2)

    val indexLookup = itemsMap.keys.zip(indexes).toMap
    val items = itemsMap.values.toSeq

    f(items).flatMap { updatedItems =>
      val updatedItemsMap = buildItemsMap(updatedItems)

      val updatedItemsWithIndex = indexLookup.map { case (srcId, index) =>
        updatedItemsMap.get(srcId).map { updatedItem => (updatedItem, index) }
      } flatten

      if(updatedItemsWithIndex.size != itemsWithIndex.size) {
        Future.failed(new RuntimeException(
          s"Inconsistent results when trying to update items! Received: ${itemsWithIndex}, updated: ${updatedItemsWithIndex}"
        ))
      } else {
        Future.successful(updatedItemsWithIndex)
      }
    }
  }

  def updateItems(
    work: Work.Visible[WorkState.Indexed]
  ): Future[Iterable[Item[IdState.Minted]]] =
    Future.sequence {
      work.data.items.zipWithIndex.groupBy {
        case (Item(IdState.Identified(_, srcId, _), _, _, _), _) => srcId.identifierType
      } map {
        case (IdentifierType.SierraSystemNumber, itemsWithIndex) =>
          preserveOrder(itemsWithIndex, updateSierraItem)
        // In the future if we wish to update other sources we can add that here
        case (_, itemsWithIndex) => Future(itemsWithIndex)
      }
      // unzipWithIndex
    } map(_.flatten.toList.sortBy(_._2).map(_._1))
}
