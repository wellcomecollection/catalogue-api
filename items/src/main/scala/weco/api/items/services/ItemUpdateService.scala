package weco.api.items.services

import weco.catalogue.internal_model.identifiers.{IdState, IdentifierType, SourceIdentifier}
import weco.catalogue.internal_model.work.{Item, Work, WorkState}
import scala.concurrent.{ExecutionContext, Future}

trait ItemUpdater {
  val identifierType: IdentifierType
  def updateItems(items: Seq[Item[IdState.Minted]]): Future[Seq[Item[IdState.Minted]]]
}

class ItemUpdateService(
  itemUpdaters: List[ItemUpdater]
)(implicit executionContext: ExecutionContext) {

  private final val itemUpdatesMap = itemUpdaters
    .map(updater => updater.identifierType -> updater)
    .toMap

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
        case (identifierType, itemsWithIndex) => itemUpdatesMap
          .get(identifierType)
          .map(updater => preserveOrder(itemsWithIndex, updater.updateItems))
          .getOrElse(Future(itemsWithIndex))
      }
      // unzipWithIndex
    } map(_.flatten.toList.sortBy(_._2).map(_._1))
}
