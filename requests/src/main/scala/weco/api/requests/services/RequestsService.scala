package weco.api.requests.services

import grizzled.slf4j.Logging
import weco.api.requests.models.{HoldAccepted, HoldRejected, RequestedItemWithWork}
import weco.api.search.elasticsearch.ElasticsearchError
import weco.api.requests.models.HoldRejected.SourceSystemNotSupported
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraSystemNumber
import weco.sierra.models.fields.SierraHold
import weco.sierra.models.identifiers.SierraPatronNumber

import scala.concurrent.{ExecutionContext, Future}

class RequestsService(
  sierraService: SierraRequestsService,
  itemLookup: ItemLookup,
)(implicit executionContext: ExecutionContext)
    extends Logging {

  def makeRequest(
    itemId: CanonicalId,
    patronNumber: SierraPatronNumber
  ): Future[Either[HoldRejected, HoldAccepted]] =
    itemLookup.byCanonicalId(itemId).flatMap {
      case Right(item)
          if item.id.sourceIdentifier.identifierType == SierraSystemNumber =>
        sierraService.placeHold(
          patron = patronNumber,
          sourceIdentifier = item.id.sourceIdentifier
        )

      case Right(sourceIdentifier) =>
        warn(s"Cannot request from source: $itemId / $sourceIdentifier")
        Future.successful(Left(SourceSystemNotSupported))

      case Left(err: ElasticsearchError) =>
        error(s"Failed to do itemLookup: $itemId", err)
        Future.failed(err)
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
