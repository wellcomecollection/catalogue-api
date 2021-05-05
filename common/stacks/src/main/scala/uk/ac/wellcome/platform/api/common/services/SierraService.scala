package uk.ac.wellcome.platform.api.common.services

import java.time.Instant
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.api.common.models._
import uk.ac.wellcome.platform.api.common.services.source.SierraSource
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraIdentifier
import weco.catalogue.internal_model.identifiers.SourceIdentifier

import scala.concurrent.{ExecutionContext, Future}

sealed trait HoldResponse {
  val lastModified: Instant
}
case class HoldAccepted(lastModified: Instant = Instant.now())
    extends HoldResponse
case class HoldRejected(lastModified: Instant = Instant.now())
    extends HoldResponse

class SierraService(
  sierraSource: SierraSource
)(implicit ec: ExecutionContext)
    extends Logging {

  import SierraSource._

  def getItemStatus(
    sierraId: SierraItemIdentifier
  ): Future[StacksItemStatus] =
    sierraSource
      .getSierraItemStub(sierraId)
      .map(item => StacksItemStatus(item.status.code))

  def placeHold(
    userIdentifier: StacksUserIdentifier,
    sierraItemIdentifier: SierraItemIdentifier,
    neededBy: Option[Instant]
  ): Future[HoldResponse] =
    sierraSource
      .postHold(
        userIdentifier,
        sierraItemIdentifier,
        neededBy
      ) map {
      // This is an "XCirc/Record not available" error
      // See https://techdocs.iii.com/sierraapi/Content/zReference/errorHandling.htm
      case Left(SierraErrorCode(132, 2, 500, _, _)) => HoldRejected()

      case Left(result) =>
        warn(s"Unrecognised hold error: $result")
        HoldRejected()

      case Right(_) => HoldAccepted()
    }

  protected def buildStacksHold(
    entry: SierraUserHoldsEntryStub
  ): StacksHold = {

    val itemId = SourceIdentifier(
      identifierType = SierraIdentifier,
      ontologyType = "Item",
      // The values from the Sierra API are of the form
      // https://libsys.wellcomelibrary.org/iii/sierra-api/v5/items/1292185
      //
      // For now, just split on slashes to extract the ID.
      value = entry.record.split("/").last
    )

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

    StacksHold(id = None, itemId, pickup, status)
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
