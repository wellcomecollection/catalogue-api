package uk.ac.wellcome.platform.api.common.services

import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.api.common.models._
import uk.ac.wellcome.platform.api.common.services.source.SierraSource
import weco.api.stacks.models.{HoldAccepted, HoldRejected, HoldResponse, SierraItemIdentifier, SierraItemNumber, StacksUserIdentifier}
import weco.catalogue.internal_model.identifiers.{IdentifierType, SourceIdentifier}

import scala.concurrent.{ExecutionContext, Future}

class SierraService(
  sierraSource: SierraSource
)(implicit ec: ExecutionContext)
    extends Logging {

  import SierraSource._

  private def isSierraItemId(sourceIdentifier: SourceIdentifier): Boolean =
    (sourceIdentifier.identifierType == IdentifierType.SierraSystemNumber) &&
      (sourceIdentifier.ontologyType == "Item")

  def getItemStatus(
    sourceIdentifier: SourceIdentifier): Future[StacksItemStatus] = {
    require(isSierraItemId(sourceIdentifier))

    val itemNumber = SierraItemNumber(sourceIdentifier.value)

    sierraSource
      .getSierraItemStub(itemNumber)
      .map(item => StacksItemStatus(item.status.code))
  }

  def placeHold(
    userIdentifier: StacksUserIdentifier,
    sourceIdentifier: SourceIdentifier
  ): Future[HoldResponse] = {
    require(isSierraItemId(sourceIdentifier))

    val itemNumber = SierraItemNumber(sourceIdentifier.value)

    sierraSource.postHold(userIdentifier, itemNumber) map {
      // This is an "XCirc/Record not available" error
      // See https://techdocs.iii.com/sierraapi/Content/zReference/errorHandling.htm
      case Left(SierraErrorCode(132, 2, 500, _, _)) => HoldRejected()

      case Left(result) =>
        warn(s"Unrecognised hold error: $result")
        HoldRejected()

      case Right(_) => HoldAccepted()
    }
  }

  protected def buildStacksHold(
    entry: SierraUserHoldsEntryStub
  ): StacksHold = {

    val sourceIdentifier = SierraItemIdentifier
      .createFromSierraId(entry.record)

    val pickupLocation = StacksPickupLocation(
      id = entry.pickupLocation.code,
      label = entry.pickupLocation.name
    )

    val pickup = StacksPickup(
      location = pickupLocation,
      pickUpBy = entry.pickupByDate
    )

    val status = StacksHoldStatus(
      id = entry.status.code,
      label = entry.status.name
    )

    StacksHold(sourceIdentifier, pickup, status)
  }

  def getStacksUserHolds(
    userId: StacksUserIdentifier
  ): Future[StacksUserHolds] = {
    sierraSource
      .getSierraUserHoldsStub(userId)
      .map { hold =>
        StacksUserHolds(
          userId = userId.value,
          holds = hold.entries.map(buildStacksHold)
        )
      }
  }
}
