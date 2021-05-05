package uk.ac.wellcome.platform.api.common.services

import java.time.Instant
import cats.instances.future._
import cats.instances.list._
import cats.syntax.traverse._
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.api.common.models._
import weco.catalogue.internal_model.identifiers.{
  CanonicalId,
  IdState,
  SourceIdentifier
}
import weco.catalogue.internal_model.work.{Item, Work}
import weco.catalogue.internal_model.work.WorkState.Indexed

import scala.concurrent.{ExecutionContext, Future}

class StacksService(
  catalogueService: CatalogueService,
  val sierraService: SierraService
)(
  implicit ec: ExecutionContext
) extends Logging {

  def requestHoldOnItem(
    userIdentifier: StacksUserIdentifier,
    itemId: CanonicalId,
    neededBy: Option[Instant]
  ): Future[HoldResponse] =
    for {
      stacksItem <- catalogueService.getStacksItemFromItemId(itemId)

      response <- stacksItem match {
        case Some(id) =>
          sierraService.placeHold(
            userIdentifier = userIdentifier,
            sierraItemIdentifier = id.sierraId,
            neededBy = neededBy
          )
        case None =>
          Future.failed(
            new Exception(f"Could not locate item $itemId!")
          )
      }

    } yield response

  def requestHoldOnItem(
    userIdentifier: StacksUserIdentifier,
    item: Item[IdState.Identified],
    neededBy: Option[Instant]
  ): Future[HoldResponse] =
    catalogueService.getSierraItemIdentifierNew(item) match {
      case Some(SourceIdentifier(_, _, value)) =>
        sierraService.placeHold(
          userIdentifier = userIdentifier,
          sierraItemIdentifier = SierraItemIdentifier(value.toLong),
          neededBy = neededBy
        )

      case None =>
        Future.failed(
          new Exception(s"Could not find a Sierra ID on ${item.id}!"))
    }

  def getStacksWork(work: Work.Visible[Indexed]): Future[StacksWork] = {
    val itemIds = catalogueService.getAllStacksItemsFromWorkNew(work)

    for {
      itemStatuses <- itemIds
        .map(_.sierraId)
        .traverse(sierraService.getItemStatus)

      stacksItemsWithStatuses = (itemIds zip itemStatuses) map {
        case (itemId, status) => StacksItem(itemId, status)
      }
    } yield StacksWork(work.state.canonicalId, stacksItemsWithStatuses)
  }

  def getStacksWork(
    workId: CanonicalId
  ): Future[StacksWork] =
    for {
      stacksItemIds <- catalogueService.getAllStacksItemsFromWork(workId)

      itemStatuses <- stacksItemIds
        .map(_.sierraId)
        .traverse(sierraService.getItemStatus)

      stacksItemsWithStatuses = (stacksItemIds zip itemStatuses) map {
        case (itemId, status) => StacksItem(itemId, status)
      }

    } yield StacksWork(workId, stacksItemsWithStatuses)
}
