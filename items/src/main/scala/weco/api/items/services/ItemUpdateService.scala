package weco.api.items.services

import weco.catalogue.internal_model.identifiers.{
  IdState,
  IdentifierType,
  SourceIdentifier
}
import weco.catalogue.internal_model.work.{Item, Work, WorkState}

import scala.concurrent.{ExecutionContext, Future}

trait ItemUpdater {
  val identifierType: IdentifierType
  def updateItems(
    items: Seq[Item[IdState.Identified]]
  ): Future[Seq[Item[IdState.Identified]]]
}

class ItemUpdateService(
  itemUpdaters: List[ItemUpdater]
)(implicit executionContext: ExecutionContext) {

  private final val itemUpdatesMap = itemUpdaters
    .map(updater => updater.identifierType -> updater)
    .toMap

  private def buildItemsMap(
    items: Seq[Item[IdState.Identified]]
  ): Map[SourceIdentifier, Item[IdState.Identified]] =
    items.map {
      case item @ Item(IdState.Identified(_, srcId, _), _, _, _) =>
        srcId -> item
    } toMap

  private def preserveOrder(
    itemsWithIndex: Seq[(Item[IdState.Identified], Int)],
    updateFunction: Seq[Item[IdState.Identified]] => Future[
      Seq[Item[IdState.Identified]]
    ]
  ) = {
    val items = itemsWithIndex.map(_._1)
    val indexes = itemsWithIndex.map(_._2)

    val itemsMap = buildItemsMap(items)
    val indexLookup = itemsMap.keys.zip(indexes).toMap

    updateFunction(items).flatMap { updatedItems =>
      val updatedItemsMap = buildItemsMap(updatedItems)
      val updatedItemsWithIndex = indexLookup.map {
        case (srcId, index) =>
          updatedItemsMap.get(srcId).map { updatedItem =>
            (updatedItem, index)
          }
      } flatten

      if (updatedItemsWithIndex.size != itemsWithIndex.size) {
        Future.failed(
          new RuntimeException(
            s"Inconsistent results when trying to update items! Received: ${itemsWithIndex}, updated: ${updatedItemsWithIndex}"
          )
        )
      } else {
        Future.successful(updatedItemsWithIndex)
      }
    }
  }

  def updateItems(
    work: Work.Visible[WorkState.Indexed]
  ): Future[Seq[Item[IdState.Minted]]] =
    Future.sequence {
      work.data.items.zipWithIndex.groupBy {
        case (Item(IdState.Identified(_, srcId, _), _, _, _), _) =>
          Some(srcId.identifierType)
        case (Item(IdState.Unidentifiable, _, _, _), _) => None
      } map {
        // To satisfy the compiler when calling preserveOrder we need to add a type for itemsWithIndex
        // The above groupBy ensures ONLY IdState.Identified is handed here, unknown to the compiler
        // A further complication of type erasure means that we must acknowledge that Seq[_] is unchecked
        case (
            Some(identifierType),
            itemsWithIndex: Seq[(Item[IdState.Identified], Int) @unchecked]
            ) =>
          itemUpdatesMap
            .get(identifierType)
            .map(updater => preserveOrder(itemsWithIndex, updater.updateItems))
            .getOrElse(Future(itemsWithIndex))
        case (None, itemsWithIndex) =>
          Future(itemsWithIndex)
      }
      // unzipWithIndex
    } map (_.flatten.toList.sortBy(_._2).map(_._1))
}
