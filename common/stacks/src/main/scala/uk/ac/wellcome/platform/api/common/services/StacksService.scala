package uk.ac.wellcome.platform.api.common.services

import cats.instances.future._
import cats.instances.list._
import cats.syntax.traverse._
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.api.common.models._

import scala.concurrent.{ExecutionContext, Future}

class StacksService(
  catalogueService: CatalogueService,
  sierraService: SierraService
)(
  implicit ec: ExecutionContext
) extends Logging {
  def getStacksUserHolds(
    userId: StacksUserIdentifier
  ): Future[StacksUserHolds] =
    for {
      userHolds <- sierraService.getStacksUserHolds(userId)
      stacksItemIds <- userHolds.holds
        .map(_.itemId)
        .traverse(catalogueService.getStacksItem)

      updatedUserHolds = (userHolds.holds zip stacksItemIds) map {
        case (hold, Some(stacksItemId)) =>
          Some(hold.copy(itemId = stacksItemId))
        case (hold, None) =>
          error(f"Unable to map $hold to Catalogue Id!")
          None
      }

    } yield userHolds.copy(holds = updatedUserHolds.flatten)
}
