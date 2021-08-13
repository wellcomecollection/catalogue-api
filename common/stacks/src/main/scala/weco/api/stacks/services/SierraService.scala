package weco.api.stacks.services

import akka.stream.Materializer
import grizzled.slf4j.Logging
import weco.api.stacks.http.{
  SierraItemDataEntries,
  SierraItemLookupError,
  SierraSource
}
import weco.api.stacks.models._
import weco.catalogue.internal_model.identifiers.SourceIdentifier
import weco.catalogue.internal_model.locations.{
  AccessCondition,
  AccessMethod,
  PhysicalLocationType
}
import weco.catalogue.source_model.sierra.rules.{
  SierraItemAccess,
  SierraPhysicalLocationType
}
import weco.http.client.{HttpClient, HttpGet, HttpPost}
import weco.sierra.models.data.SierraItemData
import weco.sierra.models.identifiers.{
  SierraBibNumber,
  SierraItemNumber,
  SierraPatronNumber
}

import scala.concurrent.{ExecutionContext, Future}

/** @param holdLimit What's the most items a single user can have on hold at once?
  *                   TODO: Make this a configurable parameter.
  *
  */
class SierraService(
  sierraSource: SierraSource,
  holdLimit: Int = 10
)(implicit ec: ExecutionContext)
    extends Logging {

  def getAccessConditions(
    itemNumbers: Seq[SierraItemNumber]
  ): Future[Map[SierraItemNumber, AccessCondition]] = {
    for {
      itemEither <- sierraSource.lookupItemEntries(itemNumbers)

      accessConditions = itemEither match {
        case Right(SierraItemDataEntries(_, _, entries)) =>
          entries.map(item => item.id -> item.getAccessCondition).toMap
        case Left(
            SierraItemLookupError.MissingItems(missingItems, itemsReturned)
            ) =>
          warn(s"Item lookup missing items: ${missingItems}")
          itemsReturned.map(item => item.id -> item.getAccessCondition).toMap
        case Left(itemLookupError) =>
          error(s"Item lookup failed: ${itemLookupError}")
          Map.empty[SierraItemNumber, AccessCondition]
      }
    } yield accessConditions
  }

  def placeHold(
    patron: SierraPatronNumber,
    sourceIdentifier: SourceIdentifier
  ): Future[Either[HoldRejected, HoldAccepted]] = {
    val item = SierraItemIdentifier.fromSourceIdentifier(sourceIdentifier)

    sierraSource.createHold(patron, item).flatMap {
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
      case Left(SierraErrorCode(132, specificCode, 500, _, _))
          if specificCode == 2 || specificCode == 929 =>
        getHolds(patron).flatMap {
          case holds if holds.size >= holdLimit =>
            Future.successful(Left(HoldRejected.UserIsAtHoldLimit))
          case holds if holds.keys.toList.contains(sourceIdentifier) =>
            Future.successful(Right(HoldAccepted.HoldAlreadyExists))
          case _ => checkIfItemCanBeRequested(item)
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
        checkIfItemCanBeRequested(item)

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
  ): Future[Either[HoldRejected, HoldAccepted]] =
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
        Left(HoldRejected.ItemCannotBeRequested)

      // If the holdCount is non-zero, that means another user has a hold on this item,
      // and only a single user can have an item requested at a time.
      //
      // By this point, we've already checked the list of holds for this user -- since they
      // don't have it, this item must be on hold for another user.
      case Right(SierraItemData(_, _, _, _, Some(holdCount), _, _, _))
          if holdCount > 0 =>
        Left(HoldRejected.ItemIsOnHoldForAnotherUser)

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
        Left(HoldRejected.ItemMissingFromSourceSystem)

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
        Left(HoldRejected.ItemCannotBeRequested)

      // At this point, we've run out of reasons why Sierra didn't let us place a hold on
      // this item.
      //
      // We log a warning so we have a bit of useful debugging information to go on, and
      // then we bubble up the failure.
      case _ =>
        warn(
          s"User tried to place a hold on item $item, which failed for an unknown reason"
        )
        Left(HoldRejected.UnknownReason)
    }

  implicit class ItemDataOps(itemData: SierraItemData) {
    def getAccessCondition: AccessCondition = {

      val location: Option[PhysicalLocationType] =
        itemData.fixedFields
          .get("79")
          .flatMap(_.display)
          .flatMap(
            name => SierraPhysicalLocationType.fromName(itemData.id, name)
          )

      // The bib ID is used for debugging purposes; the bib status is only used
      // for consistency checking. We can use placeholder data here.
      val (ac, _) = SierraItemAccess(
        bibId = SierraBibNumber("0000000"),
        bibStatus = None,
        location = location,
        itemData = itemData
      )

      ac
    }

    def allowsOnlineRequesting: Boolean = {
      val accessCondition = getAccessCondition
      accessCondition.method == AccessMethod.OnlineRequest
    }
  }

  def getHolds(patronNumber: SierraPatronNumber): Future[Map[SourceIdentifier, SierraHold]] =
    for {
      holds <- sierraSource
        .listHolds(patronNumber)
        .flatMap {
          case Right(holds) => Future(holds)
          case Left(sierraError) =>
            error(s"Failed to list holds for patron ${patronNumber} in Sierra, got: ${sierraError}")

            Future.failed(new RuntimeException(
              s"Sierra error trying to retrieve holds!"
            ))
        }

      sourceIdentifiers = holds.entries.map { hold =>
        SierraItemIdentifier.toSourceIdentifier(
          SierraItemIdentifier.fromUrl(hold.record)
        )
      }
    } yield sourceIdentifiers.zip(holds.entries).toMap
}

object SierraService {
  def apply(client: HttpClient with HttpGet with HttpPost, holdLimit: Int = 10)(
    implicit
    ec: ExecutionContext,
    mat: Materializer
  ): SierraService =
    new SierraService(
      new SierraSource(client),
      holdLimit = holdLimit
    )
}
