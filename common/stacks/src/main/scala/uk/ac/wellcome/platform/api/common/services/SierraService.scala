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

      // API error code "132" means "Request denied by XCirc".  This is listed in
      // the Sierra documentation:
      // https://techdocs.iii.com/sierraapi/Content/zReference/errorHandling.htm
      //
      // Unfortunately I don't know all the specific XCirc error codes, and I can't
      // find any documentation describing them.  Here are the ones we know:
      //
      //    2 = "Record not available"
      //        This can mean the item is already on hold, possibly by this user.
      //        It may also mean you're not allowed to request this item.
      //
      //    433 = "Bib record cannot be loaded"
      //        This can mean you tried to request an item that doesn't exist in
      //        Sierra.
      //
      //    929 = "Your request has already been sent"
      //        This appears when you send the same hold request multiple times in
      //        quick succession.
      //
      // For examples of error responses taken from real Sierra requests, see the
      // test cases in RequestingScenarioTest.

      // If the hold fails or there's a suggestion that we might have already placed
      // a hold on this item, look up what holds this user already has.
      //
      // We might find:
      //
      //    - They already have a hold on this item, in which case we report the
      //      hold as successful, even if it was really a no-op.
      //    - They're at their hold limit, so they can't request new items (regardless
      //      of whether this particular item can be requested)
      //    - They're not at their hold limit and they don't have a hold on this item --
      //      so we look to see if this item can be requested.
      //
      case Left(SierraErrorCode(132, specificCode, 500, _, _)) if specificCode == 2 || specificCode == 929 =>
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

      // If the hold fails because the bib record couldn't be loaded, that's a strong
      // suggestion that the item doesn't exist.
      //
      // We could bail out immediately, but because the Sierra error codes aren't documented
      // and it should be quite unusual to hit this error path, we go ahead and check if the
      // item can be requested, just to be sure.
      //
      // If the item really doesn't exist, we'll find out pretty quickly.
      //
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
