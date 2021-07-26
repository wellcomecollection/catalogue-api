package weco.api.items.services

import weco.catalogue.internal_model.identifiers.{IdState, SourceIdentifier}
import weco.catalogue.internal_model.work.{Item, Work, WorkState}

import scala.concurrent.{ExecutionContext, Future}

/** A service for updating the items on a Work
  *
  *  @param itemUpdaters a list of ItemUpdater for updating items of particular IdentifierType
  */
class ItemUpdateService(
  itemUpdaters: List[ItemUpdater]
)(implicit executionContext: ExecutionContext) {

  type ItemsWithIndex = Seq[(Item[IdState.Identified], Int)]

  private final val itemUpdatesMap = itemUpdaters
    .map(updater => updater.identifierType -> updater)
    .toMap

  def getSrcId(item: Item[IdState.Identified]): SourceIdentifier =
    item match {
      case Item(IdState.Identified(_, srcId, _), _, _, _) => srcId
    }

  /** Updates a tuple of Item and index preserving the original index
    *
    *  @return a list of updated items with their index maintained
    */
  private def preservedOrderItemsUpdate(
    itemsWithIndex: ItemsWithIndex,
    updateFunction: Seq[Item[IdState.Identified]] => Future[
      Seq[Item[IdState.Identified]]
    ]
  ): Future[ItemsWithIndex] = {
    val items = itemsWithIndex.map(_._1)

    updateFunction(items).map { updatedItems =>
      // Construct a lookup from SourceIdentifier -> index
      val updatedItemsWithIndex = itemsWithIndex
        .map {
          case (item, index) => getSrcId(item) -> index
        }
        .flatMap {
          // Add the correct index for an item by SourceIdentifier
          case (srcId, index) =>
            updatedItems.find(_.id.sourceIdentifier == srcId).map {
              updatedItem =>
                (updatedItem, index)
            }
        }

      // Ensure that the update function has updated the correct number of results
      require(
        updatedItemsWithIndex.size == itemsWithIndex.size,
        "Inconsistent results updating items: " +
          s"Received: ${itemsWithIndex}, updated: ${updatedItemsWithIndex}"
      )

      updatedItemsWithIndex
    }
  }

  /** Updates the Identified items on a work
    *
    *  Uses an ItemUpdater to update Identified items
    *  where the ItemUpdater acts on a specific IdentifierType
    *
    *  @return a sequence of updated items
    */
  def updateItems(
    work: Work.Visible[WorkState.Indexed]
  ): Future[Seq[Item[IdState.Minted]]] =
    Future.sequence {
      // Group our results by IdentifierType or where Unidentifiable
      work.data.items.zipWithIndex.groupBy {
        case (Item(IdState.Identified(_, srcId, _), _, _, _), _) =>
          Some(srcId.identifierType)
        case (Item(IdState.Unidentifiable, _, _, _), _) => None
      } map {
        /* To satisfy the compiler when calling preserveOrder we need to add a type for itemsWithIndex
         * The above groupBy ensures ONLY IdState.Identified is handed here, unknown to the compiler
         * A further complication of type erasure means that we must acknowledge that Seq[_] is unchecked
         */
        case (
            Some(identifierType),
            itemsWithIndex: Seq[(Item[IdState.Identified], Int) @unchecked]
            ) =>
          // Look for an ItemUpdater for this IdentifierType and update
          itemUpdatesMap
            .get(identifierType)
            .map(
              updater =>
                preservedOrderItemsUpdate(
                  itemsWithIndex = itemsWithIndex,
                  updateFunction = updater.updateItems
                )
            )
            .getOrElse(Future(itemsWithIndex))
        case (None, itemsWithIndex) =>
          Future(itemsWithIndex)
      }
      // unzipWithIndex
    } map (_.flatten.toList.sortBy {
      case (_, index) => index
    } map {
      case (item, _) => item
    })
}
