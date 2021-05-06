package weco.api.stacks.items.responses

import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import uk.ac.wellcome.platform.api.common.models.display.DisplayStacksWork
import uk.ac.wellcome.platform.api.common.models.{StacksItem, StacksWork}
import uk.ac.wellcome.platform.api.common.services.SierraService
import uk.ac.wellcome.platform.api.rest.SingleWorkDirectives
import weco.api.stacks.services.WorkLookup
import weco.catalogue.internal_model.identifiers.{CanonicalId, IdState, IdentifierType}
import weco.catalogue.internal_model.work.{Item, Work, WorkState}

import scala.concurrent.Future

trait LookupItemStatus extends SingleWorkDirectives {
  val workLookup: WorkLookup
  val sierraService: SierraService
  val index: Index

  def lookupStatus(workId: CanonicalId): Future[Route] =
    workLookup.byCanonicalId(workId)(index)
      .mapVisible { work: Work.Visible[WorkState.Indexed] =>
        val items =
          work.data.items
            .collect {
              case Item(IdState.Identified(canonicalId, sourceIdentifier, _), _, _)
                if sourceIdentifier.identifierType == IdentifierType.SierraSystemNumber =>
                  (canonicalId, sourceIdentifier)
            }

        for {
          stacksItems <- Future.sequence(
            items.map { case (canonicalId, sourceIdentifier) =>
              sierraService.getItemStatus(sourceIdentifier)
                .map { result => StacksItem(canonicalId, sourceIdentifier, result) }
            }
          )

          stacksWork = StacksWork(workId, stacksItems)

          displayWork = DisplayStacksWork(stacksWork)
        } yield complete(displayWork)
      }
}
