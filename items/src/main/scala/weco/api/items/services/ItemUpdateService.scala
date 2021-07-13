package weco.api.items.services

import grizzled.slf4j.Logging
import weco.api.stacks.services.SierraService
import weco.catalogue.internal_model.identifiers.{
  IdState,
  IdentifierType,
  SourceIdentifier
}
import weco.catalogue.internal_model.locations.{
  AccessCondition,
  PhysicalLocation
}
import weco.catalogue.internal_model.work.{Item, Work, WorkState}

import scala.concurrent.{ExecutionContext, Future}

class ItemUpdateService(
  sierraService: SierraService
)(implicit executionContext: ExecutionContext)
    extends Logging {

  private def isSierraId(sourceIdentifier: SourceIdentifier) =
    sourceIdentifier.identifierType == IdentifierType.SierraSystemNumber

  private def updateAccessCondition(
    item: Item[IdState.Minted],
    accessConditionOption: Option[AccessCondition]
  ) =
    item.locations.map {
      case physicalLocation: PhysicalLocation =>
        physicalLocation.copy(
          accessConditions = accessConditionOption.toList
        )
      case location => location
    }

  private def refreshItem(srcId: SourceIdentifier, item: Item[IdState.Minted]) =
    sierraService
      .getAccessCondition(srcId)
      .map {
        case Right(accessConditionOption) =>
          item.copy(
            locations = updateAccessCondition(item, accessConditionOption)
          )

        case Left(err) =>
          error(msg = f"Couldn't refresh item: ${item.id} got error ${err}")
          item
      }

  def updateItems(
    work: Work.Visible[WorkState.Indexed]
  ): Future[List[Item[IdState.Minted]]] = Future.sequence {
    work.data.items.map {
      case item @ Item(IdState.Identified(_, srcId, _), _, _, _)
          if isSierraId(srcId) =>
        refreshItem(srcId, item)
      case item => Future(item)
    }
  }
}
