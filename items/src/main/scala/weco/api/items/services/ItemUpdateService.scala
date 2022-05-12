package weco.api.items.services

import io.circe.Json
import weco.api.items.json.JsonOps
import weco.api.stacks.models.CatalogueWork

import scala.concurrent.{ExecutionContext, Future}

/** A service for updating the items on a Work
  *
  *  @param itemUpdaters a list of ItemUpdater for updating items of particular IdentifierType
  */
class ItemUpdateService(
  itemUpdaters: List[ItemUpdater]
)(implicit executionContext: ExecutionContext) extends JsonOps {

  type ItemsWithIndex = Seq[(Json, Int)]

  private final val itemUpdatesMap = itemUpdaters
    .map(updater => updater.identifierType.id -> updater)
    .toMap

  /** Updates a tuple of Item and index preserving the original index
    *
    *  @return a list of updated items with their index maintained
    */
  private def preservedOrderItemsUpdate(
    itemsWithIndex: ItemsWithIndex,
    updateFunction: Seq[Json] => Future[Seq[Json]]
  ): Future[ItemsWithIndex] = {
    val items = itemsWithIndex.map(_._1)

    updateFunction(items).map { updatedItems =>
      // Construct a lookup from SourceIdentifier -> index
      val updatedItemsWithIndex = itemsWithIndex
        .map {
          case (item, index) => item.identifier -> index
        }
        .flatMap {
          // Add the correct index for an item by SourceIdentifier
          case (srcId, index) =>
            updatedItems.find(_.identifier == srcId).map { updatedItem =>
              (updatedItem, index)
            }
        }

      // Ensure that the update function has updated the correct number of results
      require(
        updatedItemsWithIndex.size == itemsWithIndex.size,
        "Inconsistent results updating items: " +
          s"Received: $itemsWithIndex, updated: $updatedItemsWithIndex"
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
  def updateItems(work: CatalogueWork): Future[Seq[Json]] = {
    val items = work.items

    val groupedItems = items.zipWithIndex.groupBy {
      case (item, _) => item.identifierType
    }

    Future.sequence {
      groupedItems.map {
        case (Some(identifierType), itemsWithIndex) =>
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
}
