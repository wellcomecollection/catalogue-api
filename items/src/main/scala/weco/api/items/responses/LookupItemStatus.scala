package weco.api.items.responses

import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import weco.api.search.rest.SingleWorkDirectives
import weco.api.stacks.services.{SierraService, WorkLookup}
import weco.catalogue.display_model.models.DisplayItem
import weco.catalogue.internal_model.locations.{AccessCondition, PhysicalLocation}
import weco.catalogue.internal_model.identifiers.{
  CanonicalId,
  IdState,
  IdentifierType,
  SourceIdentifier
}
import weco.catalogue.internal_model.locations.PhysicalLocation
import weco.catalogue.internal_model.work.{Item, Work, WorkState}

import scala.concurrent.Future

case class DisplayItemResponse(items: Seq[DisplayItem])

trait LookupItemStatus extends SingleWorkDirectives {
  val workLookup: WorkLookup
  val sierraService: SierraService
  val index: Index

  private def isSierraId(sourceIdentifier: SourceIdentifier) =
    sourceIdentifier.identifierType == IdentifierType.SierraSystemNumber

  private def updateAccessCondition(item: Item[IdState.Minted], accessConditionOption: Option[AccessCondition]) =
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
          item.copy(locations = updateAccessCondition(item, accessConditionOption))

        case Left(err) =>
          error(msg = f"Couldn't refresh item: ${item.id} got error ${err}")
          item
      }

  def lookupStatus(workId: CanonicalId): Future[Route] =
    workLookup
      .byCanonicalId(workId)(index)
      .mapVisible { work: Work.Visible[WorkState.Indexed] =>
        val futureItems = work.data.items.map {
          case item @ Item(IdState.Identified(_, srcId, _), _, _, _)
              if isSierraId(srcId) =>
            sierraService
              .getAccessCondition(srcId)
              .map {
                case Right(accessConditionOption) => {
                  val locations = item.locations.map {
                    case physicalLocation: PhysicalLocation =>
                      physicalLocation.copy(
                        accessConditions = accessConditionOption.toList)
                    case location => location
                  }

                  item.copy(locations = locations)
                }
                case Left(err) => {
                  error(
                    msg = f"Couldn't refresh item: ${item.id} got error ${err}")

                  item
                }
              }
          case item => Future(item)
        }

        for {
          items: Seq[Item[IdState.Minted]] <- Future.sequence(futureItems)
          displayItems = items.map(item => DisplayItem(item, true))
          displayItemResponse = DisplayItemResponse(displayItems)

        } yield complete(displayItemResponse)
      }
}
