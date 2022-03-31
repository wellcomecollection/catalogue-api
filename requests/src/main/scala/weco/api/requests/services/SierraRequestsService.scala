package weco.api.requests.services

import akka.stream.Materializer
import grizzled.slf4j.Logging
import weco.api.requests.models.{HoldAccepted, HoldRejected}
import weco.api.stacks.models.SierraItemIdentifier
import weco.catalogue.internal_model.identifiers.SourceIdentifier
import weco.http.client.{HttpClient, HttpGet, HttpPost}
import weco.sierra.http.SierraSource
import weco.sierra.models.data.SierraItemData
import weco.sierra.models.errors.{SierraErrorCode, SierraItemLookupError}
import weco.sierra.models.fields.SierraHold
import weco.sierra.models.identifiers.{SierraItemNumber, SierraPatronNumber}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

/** @param holdLimit What's the most items a single user can have on hold at once?
  *
  */
class SierraRequestsService(
  sierraSource: SierraSource,
  holdLimit: Int
)(implicit ec: ExecutionContext)
    extends Logging {

  import weco.api.stacks.models.SierraItemDataOps._

  def placeHold(
    patron: SierraPatronNumber,
    pickupDate: Option[LocalDate],
    sourceIdentifier: SourceIdentifier
  ): Future[Either[HoldRejected, HoldAccepted]] = {
    val item = SierraItemIdentifier.fromSourceIdentifier(sourceIdentifier)

    sierraSource
      .createHold(
        patron = patron,
        item = item,
        note = pickupDate.map(pickupDateHoldNote)
      )
      .flatMap {
        case Right(_) => Future.successful(Right(HoldAccepted.HoldCreated))

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
        //    132 = "You may not make requests.  Please consult Enquiry Desk staff for help."
        //          This can mean you are a self registered user, or don't have a patron type,
        //          as a result you can't make requests
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
        //    - The user's patron record has expired and they can no longer make requests.
        //
        case Left(SierraErrorCode(132, 2, 500, _, Some(description)))
            if description.contains("You may not make requests") =>
          userIsSelfRegistered(patron)
            .map {
              case true  => Left(HoldRejected.UserIsSelfRegistered)
              case false => Left(HoldRejected.UnknownReason)
            }

        case Left(SierraErrorCode(132, specificCode, 500, _, _))
            if specificCode == 2 || specificCode == 929 =>
          getHolds(patron).flatMap {
            case holds if holds.keys.toList.contains(sourceIdentifier) =>
              Future.successful(Right(HoldAccepted.HoldAlreadyExists))
            case holds if holds.size >= holdLimit =>
              Future.successful(Left(HoldRejected.UserIsAtHoldLimit))
            case _ =>
              checkIfItemCanBeRequested(item)
                .flatMap {
                  case None     => checkIfUserCanMakeRequests(patron)
                  case rejected => Future.successful(rejected)
                }
                .map {
                  case Some(rejected) => Left(rejected)
                  case None           => Left(HoldRejected.UnknownReason)
                }
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
          checkIfItemCanBeRequested(item).map {
            case Some(rejected) => Left(rejected)
            case None           => Left(HoldRejected.UnknownReason)
          }

        // A 404 response from the Sierra API means the patron record doesn't exist.
        //
        // As far as I can tell, we only get this error if the patron record doesn't exist --
        // if the item record doesn't exist, we instead get the 433 error handled in the
        // previous case.
        case Left(SierraErrorCode(107, 0, 404, "Record not found", None)) =>
          Future.successful(Left(HoldRejected.UserDoesNotExist(patron)))

        case Left(result) =>
          warn(s"Unrecognised hold error: $result")
          Future.successful(Left(HoldRejected.UnknownReason))
      }
  }

  private def checkIfItemCanBeRequested(
    item: SierraItemNumber
  ): Future[Option[HoldRejected]] =
    sierraSource.lookupItem(item).map {

      // This could occur if the item has been deleted/suppressed in Sierra,
      // but that update hasn't propagated to the catalogue API yet, and we're
      // still displaying the item.
      //
      // A 404 here would be wrong -- from the perspective of the catalogue API,
      // this does exist -- so instead, we return a generic "CannotBeRequested" error.
      case Right(item) if item.deleted || item.suppressed =>
        warn(
          s"User tried to place a hold on item $item, which has been deleted/suppressed in Sierra"
        )
        Some(HoldRejected.ItemCannotBeRequested)

      // If the holdCount is non-zero, that means another user has a hold on this item,
      // and only a single user can have an item requested at a time.
      //
      // By this point, we've already checked the list of holds for this user -- since they
      // don't have it, this item must be on hold for another user.
      case Right(SierraItemData(_, _, _, _, Some(holdCount), _, _, _))
          if holdCount > 0 =>
        Some(HoldRejected.ItemIsOnHoldForAnotherUser)

      // This would be extremely unusual in practice -- when items are deleted
      // in Sierra, it's a soft delete.  The item still exists, but with "deleted: true".
      //
      // If the catalogue API points to an item that doesn't exist in Sierra,
      // it suggests something has gone badly wrong in the catalogue pipeline.
      //
      // We bubble up a 500 error, so we can be alerted and investigate further.
      case Left(SierraItemLookupError.ItemNotFound) =>
        warn(
          s"User tried to place a hold on item $item, which does not exist in Sierra"
        )
        Some(HoldRejected.ItemMissingFromSourceSystem)

      // If the rules for requesting prevent an item from being requested, we can
      // explain this to the user.
      //
      // Usually we won't display the "Online request" button for an item that doesn't
      // pass the rules for requesting.  We could hit this branch if the data has been
      // updated in Sierra to prevent requesting, but this hasn't updated in the API yet.
      case Right(itemData) if !itemData.allowsOnlineRequesting =>
        warn(
          s"User tried to place a hold on item $item, which is blocked by rules for requesting"
        )
        Some(HoldRejected.ItemCannotBeRequested)

      // At this point, we've run out of reasons why Sierra didn't let us place a hold on
      // this item.
      //
      // We log a warning so we have a bit of useful debugging information to go on, and
      // then we bubble up the failure.
      case _ =>
        warn(
          s"User tried to place a hold on item $item, which failed for an unknown reason"
        )
        None
    }

  private def userIsSelfRegistered(
    patron: SierraPatronNumber
  ): Future[Boolean] =
    sierraSource
      .lookupPatronType(patron)
      .map {
        case Right(Some(29)) => true
        case _               => false
      }

  private def checkIfUserCanMakeRequests(
    patron: SierraPatronNumber
  ): Future[Option[HoldRejected]] =
    sierraSource
      .lookupPatronExpirationDate(patron)
      .map {
        // I'm being a bit vague about the exact expiration date here, because I don't know
        // exactly how strict Sierra is and it's not worth finding out in detail.
        //
        // e.g. if your expiration date is 2001-01-01, can you make holds until the end of
        // that day, or is the last day you can make holds 2000-12-31?  I don't know.
        //
        // We just assume that an item is available, not on hold for another user, and
        // you get an otherwise unexplained hold rejection within a day or so of your
        // account expiring, that's probably the reason.
        case Right(Some(localDate))
            if localDate.isBefore(LocalDate.now().minusDays(1L)) =>
          Some(HoldRejected.UserAccountHasExpired)

        case _ => None
      }
      .recover { case _: Throwable => None }

  def getHolds(
    patronNumber: SierraPatronNumber
  ): Future[Map[SourceIdentifier, SierraHold]] =
    for {
      holds <- sierraSource
        .listHolds(patronNumber)
        .flatMap {
          case Right(holds) =>
            Future.successful(holds)
          case Left(sierraError) =>
            error(
              s"Failed to list holds for patron $patronNumber in Sierra, got: $sierraError"
            )

            Future.failed(
              new RuntimeException(
                s"Sierra error trying to retrieve holds!"
              )
            )
        }

      sourceIdentifiers = holds.entries
        .filterNot {
          // When somebody requests an item on Inter-Library Loan (ILL), a virtual record
          // is created in Sierra that tracks this request.  The record only exists for
          // as long as the loan request.
          //
          // To distinguish virtual records from regular Sierra records, they get a
          // different type of identifier: a virtual record followed by the ILL department code,
          // which in our case is "illd".  e.g. a virtual record might be 101339@illd
          //
          // There's nothing useful we can do with these items for requesting, so ignore
          // them for now.  If we don't, we get an error trying to create a Sierra identifier
          // for these records, because the ID isn't seven-digital numeric.
          //
          // See: https://documentation.iii.com/sierrahelp/Default.htm#sgcir/sgcir_ill_virtualrecs.html
          //
          // See also: https://github.com/wellcomecollection/platform/issues/5354,
          // which tracks doing something more sensible with these records.
          _.record.getPath.endsWith("@illd")
        }
        .map { hold =>
          val identifier = SierraItemIdentifier.toSourceIdentifier(
            SierraItemIdentifier.fromUrl(hold.record)
          )

          identifier -> hold
        }
    } yield sourceIdentifiers.toMap

  private def pickupDateHoldNote(pickupDate: LocalDate): String =
    s"Requested for: ${DateTimeFormatter.ofPattern("yyyy-MM-dd").format(pickupDate)}"
}

object SierraRequestsService {
  def apply(client: HttpClient with HttpGet with HttpPost, holdLimit: Int)(
    implicit
    ec: ExecutionContext,
    mat: Materializer
  ): SierraRequestsService =
    new SierraRequestsService(
      new SierraSource(client),
      holdLimit = holdLimit
    )
}
