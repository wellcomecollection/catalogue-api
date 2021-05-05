package uk.ac.wellcome.platform.api.requests.responses

import cats.syntax.traverse._
import com.sksamuel.elastic4s.{ElasticError, Index}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.api.common.models.{
  StacksUserHolds,
  StacksUserIdentifier
}
import uk.ac.wellcome.platform.api.common.services.SierraService
import weco.api.stacks.services.ItemLookup
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.work.Item

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

trait LookupHolds extends Logging {
  implicit val ec: ExecutionContext

  implicit val sierraService: SierraService
  implicit val itemLookup: ItemLookup

  val index: Index

  def lookupHolds(userId: String): Future[StacksUserHolds] = {
    val userIdentifier = StacksUserIdentifier(userId)

    for {
      userHolds <- sierraService.getStacksUserHolds(userIdentifier)

      stacksItemIds: immutable.Seq[Either[
        ElasticError,
        Option[Item[IdState.Identified]]]] <- userHolds.holds
        .map(_.itemId)
        .traverse(id => itemLookup.bySourceIdentifier(id)(index))

      updatedUserHolds = (userHolds.holds zip stacksItemIds) map {
        case (hold, Right(Some(item))) =>
          Some(hold.copy(id = Some(item.id.canonicalId)))
        case (hold, _) =>
          // TODO: Is ignoring a hold we can't find in the catalogue correct?
          error(f"Unable to map $hold to Catalogue Id!")
          None
      }
    } yield userHolds.copy(holds = updatedUserHolds.flatten)
  }
}
