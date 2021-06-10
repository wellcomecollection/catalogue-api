package uk.ac.wellcome.platform.api.common.services

import akka.stream.Materializer
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.api.common.models._
import weco.api.stacks.http.{HttpClient, SierraItemLookupError, SierraSource}
import weco.api.stacks.models.{HoldAccepted, HoldRejected, HoldResponse, SierraErrorCode, SierraHold, SierraItemIdentifier}
import weco.catalogue.internal_model.identifiers.SourceIdentifier
import weco.catalogue.source_model.sierra.identifiers.SierraPatronNumber

import scala.concurrent.{ExecutionContext, Future}

class SierraService(
  sierraSource: SierraSource
)(implicit ec: ExecutionContext)
    extends Logging {

  def getItemStatus(
    sourceIdentifier: SourceIdentifier): Future[Either[SierraItemLookupError, StacksItemStatus]] = {
    val item = SierraItemIdentifier.fromSourceIdentifier(sourceIdentifier)

    sierraSource.lookupItem(item).map {
      case Right(item) => Right(StacksItemStatus(item.status.code))
      case Left(err) => Left(err)
    }
  }

  def placeHold(
    patron: SierraPatronNumber,
    sourceIdentifier: SourceIdentifier
  ): Future[HoldResponse] = {
    val item = SierraItemIdentifier.fromSourceIdentifier(sourceIdentifier)

    sierraSource.createHold(patron, item).map {
      // This is an "XCirc/Record not available" error
      // See https://techdocs.iii.com/sierraapi/Content/zReference/errorHandling.htm
      case Left(SierraErrorCode(132, 2, 500, _, _)) => HoldRejected()

      case Left(result) =>
        warn(s"Unrecognised hold error: $result")
        HoldRejected()

      case Right(_) => HoldAccepted()
    }
  }

  protected def buildStacksHold(entry: SierraHold): StacksHold = {
    val itemNumber = SierraItemIdentifier.fromUrl(entry.record)
    val sourceIdentifier = SierraItemIdentifier.toSourceIdentifier(itemNumber)

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
    patronNumber: SierraPatronNumber
  ): Future[Either[SierraErrorCode, StacksUserHolds]] = {
    sierraSource
      .listHolds(patronNumber)
      .map {
        case Right(holds) =>
          Right(
            StacksUserHolds(
              userId = patronNumber.withoutCheckDigit,
              holds = holds.entries.map(buildStacksHold)
            )
          )

        case Left(err) => Left(err)
      }
  }
}

object SierraService {
  def apply(client: HttpClient)(implicit ec: ExecutionContext, mat: Materializer): SierraService =
    new SierraService(
      new SierraSource(client)
    )
}
