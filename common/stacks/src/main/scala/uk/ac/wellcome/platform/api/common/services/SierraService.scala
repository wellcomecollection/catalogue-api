package uk.ac.wellcome.platform.api.common.services

import akka.stream.Materializer
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.api.common.models._
import weco.api.stacks.http.{SierraItemLookupError, SierraSource}
import weco.api.stacks.models.{
  CannotBeRequested,
  HoldAccepted,
  HoldRejected,
  HoldResponse,
  SierraErrorCode,
  SierraHold,
  SierraItemIdentifier,
  UnknownError,
  UserAtHoldLimit
}
import weco.catalogue.internal_model.identifiers.SourceIdentifier
import weco.catalogue.source_model.sierra.identifiers.{
  SierraItemNumber,
  SierraPatronNumber
}
import weco.http.client.{HttpClient, HttpGet, HttpPost}

import scala.concurrent.{ExecutionContext, Future}

/** @param holdLimit What's the most items a single user can have on hold at once?
  *                  TODO: Make this a configurable parameter.
  *
  */
class SierraService(
  sierraSource: SierraSource,
  holdLimit: Int = 10
)(implicit ec: ExecutionContext)
    extends Logging {

  def getItemStatus(sourceIdentifier: SourceIdentifier)
    : Future[Either[SierraItemLookupError, StacksItemStatus]] = {
    val item = SierraItemIdentifier.fromSourceIdentifier(sourceIdentifier)

    sierraSource.lookupItem(item).map {
      case Right(item) => Right(StacksItemStatus(item.status.get.code))
      case Left(err)   => Left(err)
    }
  }

  def placeHold(
    patron: SierraPatronNumber,
    sourceIdentifier: SourceIdentifier
  ): Future[HoldResponse] = {
    val item = SierraItemIdentifier.fromSourceIdentifier(sourceIdentifier)

    sierraSource.createHold(patron, item).flatMap {
      case Right(_) => Future.successful(HoldAccepted())

      // This is an "XCirc/Record not available" error.  This can mean
      // that the item is already on hold -- possibly by this user.
      // See https://techdocs.iii.com/sierraapi/Content/zReference/errorHandling.htm
      case Left(SierraErrorCode(132, 2, 500, _, _)) =>
        getStacksUserHolds(patron).flatMap {
          case Right(holds)
              if holds.holds
                .map(_.sourceIdentifier)
                .contains(sourceIdentifier) =>
            Future.successful(HoldAccepted())

          case Right(holds) if holds.holds.size >= holdLimit =>
            Future.successful(UserAtHoldLimit())

          case _ => checkIfItemCanBeRequested(patron, item)
        }

      // This is an "XCirc/Bib record cannot be loaded" error.  We see this
      // error when you try to request an item that doesn't exist in Sierra.
      case Left(SierraErrorCode(132, 433, 500, _, _)) =>
        checkIfItemCanBeRequested(patron, item)

      case Left(result) =>
        warn(s"Unrecognised hold error: $result")
        Future.successful(HoldRejected())
    }
  }

  def checkIfItemCanBeRequested(patron: SierraPatronNumber,
                                item: SierraItemNumber): Future[HoldResponse] =
    sierraSource.lookupItem(item).map {

      // This could occur if the item has been deleted/suppressed in Sierra,
      // but that update hasn't propagated to the catalogue API yet, and we're
      // still displaying the item.
      //
      // A 404 here would be wrong -- from the perspective of the catalogue API,
      // this does exist -- so instead, we return a generic "CannotBeRequested" error.
      case Right(item) if item.deleted || item.suppressed =>
        warn(
          s"User tried to place a hold on item $item, which has been deleted/suppressed in Sierra")
        CannotBeRequested()

      // This would be extremely unusual in practice -- when items are deleted
      // in Sierra, it's a soft delete.  The item still exists, but with "deleted: true".
      //
      // If the catalogue API points to an item that doesn't exist in Sierra,
      // it suggests something has gone badly wrong in the catalogue pipeline.
      //
      // We bubble up a 500 error, so we can be alerted and investigate further.
      case Left(SierraItemLookupError.ItemNotFound) =>
        warn(
          s"User tried to place a hold on item $item, which does not exist in Sierra")
        UnknownError()

      case _ => HoldRejected()
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
  ): Future[Either[SierraErrorCode, StacksUserHolds]] =
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

object SierraService {
  def apply(client: HttpClient with HttpGet with HttpPost, holdLimit: Int = 10)(
    implicit
    ec: ExecutionContext,
    mat: Materializer): SierraService =
    new SierraService(
      new SierraSource(client),
      holdLimit = holdLimit
    )
}
