package weco.api.requests.services

import grizzled.slf4j.Logging
import weco.api.requests.models.{
  HoldAccepted,
  HoldRejected,
  RequestedItemWithWork
}
import weco.api.search.elasticsearch.ElasticsearchError
import weco.api.requests.models.HoldRejected.SourceSystemNotSupported
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
    extends Logging {

  def makeRequest(
    itemId: CanonicalId,
    pickupDate: Option[LocalDate],
    patronNumber: SierraPatronNumber
  ): Future[Either[HoldRejected, HoldAccepted]] =
    itemLookup.byCanonicalId(itemId).flatMap {
      case Right(sourceIdentifier)
          if sourceIdentifier.identifierType.id == SierraSystemNumber.id =>
        sierraService.placeHold(
          patron = patronNumber,
          sourceIdentifier = sourceIdentifier,
          pickupDate = pickupDate
        )

      case Right(sourceIdentifier) =>
        warn(s"Cannot request from source: $itemId / $sourceIdentifier")
        Future.successful(Left(SourceSystemNotSupported))

      case Left(e: ItemNotFoundError) =>
        error(s"Could not find item: $itemId", e.err)
        Future.successful(Left(HoldRejected.ItemDoesNotExist))

      case Left(e: ItemLookupError) =>
        error(s"Failed to do itemLookup: $itemId", e.err)
        Future.failed(e.err)
    }

  def getRequests(
    patronNumber: SierraPatronNumber
  ): Future[List[(SierraHold, RequestedItemWithWork)]] =
    for {
      holdsMap <- sierraService.getHolds(patronNumber)

      itemLookupResults <- itemLookup.bySourceIdentifier(holdsMap.keys.toSeq)

      itemsFound = itemLookupResults.zip(holdsMap.keys).flatMap {
        case (Right(item), _) => Some(item)
        case (Left(elasticError: ElasticsearchError), srcId) =>
          error(s"Error looking up item $srcId.", elasticError)
          None
      }

      itemHoldTuples = itemsFound.flatMap { itemLookup =>
        holdsMap.get(itemLookup.item.id.sourceIdentifier).map { hold =>
          (hold, itemLookup)
        }
      }

    } yield itemHoldTuples.toList
}
