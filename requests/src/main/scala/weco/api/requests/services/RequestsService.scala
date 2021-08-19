package weco.api.requests.services

import grizzled.slf4j.Logging
import weco.api.search.elasticsearch.ElasticsearchError
import weco.api.stacks.models.HoldRejected.SourceSystemNotSupported
import weco.api.stacks.models.{HoldAccepted, HoldRejected, SierraHold}
import weco.api.stacks.services.SierraService
import weco.catalogue.internal_model.identifiers.IdState.Identified
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraSystemNumber
import weco.catalogue.internal_model.work.Item
import weco.sierra.models.identifiers.SierraPatronNumber

import scala.concurrent.{ExecutionContext, Future}


class RequestsService(
  sierraService: SierraService,
  itemLookup: ItemLookup
)(implicit executionContext: ExecutionContext)
    extends Logging {

  def makeRequest(
    itemId: CanonicalId,
    patronNumber: SierraPatronNumber
  ): Future[Either[HoldRejected, HoldAccepted]] = {
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
  }

  def getRequests(
    patronNumber: SierraPatronNumber
  ): Future[List[(SierraHold, Item[Identified])]] = {
    for {
      holdsMap <- sierraService.getHolds(patronNumber)

      itemLookupResults <- itemLookup.bySourceIdentifier(holdsMap.keys.toSeq)

      itemsFound = itemLookupResults.zip(holdsMap.keys).flatMap {
        case (Right(item), _) => Some(item)
        case (Left(elasticError: ElasticsearchError), srcId) =>
          error(s"Error looking up item $srcId.", elasticError)
          None
      }

      itemHoldTuples = itemsFound.flatMap { item =>
        holdsMap.get(item.id.sourceIdentifier).map { hold =>
          (hold, item)
        }
      }

    } yield itemHoldTuples.toList
  }
}
