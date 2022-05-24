package weco.api.requests.services

import grizzled.slf4j.Logging
import weco.api.requests.models.HoldRejected.SourceSystemNotSupported
import weco.api.requests.models.{
  HoldAccepted,
  HoldRejected,
  RequestedItemWithWork
}
import weco.api.stacks.models.DisplayItemOps
import weco.catalogue.display_model.Implicits._
import weco.catalogue.display_model.identifiers.DisplayIdentifier
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraSystemNumber
import weco.sierra.models.fields.SierraHold
import weco.sierra.models.identifiers.SierraPatronNumber

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class RequestsService(
  sierraService: SierraRequestsService,
  itemLookup: ItemLookup
)(implicit executionContext: ExecutionContext)
    extends Logging
    with DisplayItemOps {

  def makeRequest(
    itemId: CanonicalId,
    pickupDate: Option[LocalDate],
    patronNumber: SierraPatronNumber
  ): Future[Either[HoldRejected, HoldAccepted]] =
    for {
      item <- itemLookup.byCanonicalId(itemId)

      sourceIdentifier = item match {
        case Right(item) => item.sourceIdentifier
        case _           => None
      }

      result <- (item, sourceIdentifier) match {
        case (Right(item), Some(sourceIdentifier))
            if sourceIdentifier.identifierType.id == SierraSystemNumber.id =>
          sierraService.placeHold(
            patron = patronNumber,
            sourceIdentifier = sourceIdentifier,
            pickupDate = pickupDate,
            locationType = item.physicalLocationType,
            accessCondition = item.physicalAccessCondition
          )

        case (Right(_), Some(sourceIdentifier)) =>
          warn(s"Cannot request from source: $itemId / $sourceIdentifier")
          Future.successful(Left(SourceSystemNotSupported))

        case (Left(e: ItemNotFoundError), _) =>
          error(s"Could not find item: $itemId", e.err)
          Future.successful(Left(HoldRejected.ItemDoesNotExist))

        case (Left(e: ItemLookupError), _) =>
          error(s"Failed to do itemLookup: $itemId", e.err)
          Future.failed(e.err)
      }
    } yield result

  def getRequests(
    patronNumber: SierraPatronNumber
  ): Future[List[(SierraHold, RequestedItemWithWork)]] =
    for {
      holdsMap <- sierraService.getHolds(patronNumber)
      itemLookupResults <- itemLookup.bySourceIdentifier(holdsMap.keys.toSeq)

      itemsFound = itemLookupResults.zip(holdsMap.keys).collect {
        case (Right(item), _) => item
        case (Left(itemLookupError: ItemLookupError), srcId) =>
          error(s"Error looking up item $srcId.", itemLookupError.err)
          throw itemLookupError.err
      }

      itemHoldTuples = itemsFound.flatMap { itemLookup =>
        val identifiers =
          itemLookup.item.hcursor
            .get[List[DisplayIdentifier]]("identifiers")
            .right
            .get

        val itemId = identifiers.head

        holdsMap.get(itemId).map { hold =>
          (hold, itemLookup)
        }
      }

    } yield itemHoldTuples.toList
}
