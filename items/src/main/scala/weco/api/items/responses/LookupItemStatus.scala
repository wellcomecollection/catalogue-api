package weco.api.items.responses

import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import weco.api.stacks.models.display.DisplayStacksWork
import uk.ac.wellcome.platform.api.common.models.StacksWork
import weco.api.search.rest.SingleWorkDirectives
import weco.api.stacks.models.{StacksItem, StacksWork}
import weco.api.stacks.services.{SierraService, WorkLookup}
import weco.catalogue.internal_model.identifiers.{
  CanonicalId,
  IdState,
  IdentifierType
}
import weco.catalogue.internal_model.work.{Item, Work, WorkState}

import scala.concurrent.Future

trait LookupItemStatus extends SingleWorkDirectives {
  val workLookup: WorkLookup
  val sierraService: SierraService
  val index: Index

  def lookupStatus(workId: CanonicalId): Future[Route] =
    workLookup
      .byCanonicalId(workId)(index)
      .mapVisible { work: Work.Visible[WorkState.Indexed] =>
        val items =
          work.data.items
            .collect {
              case Item(
                  IdState.Identified(canonicalId, sourceIdentifier, _),
                  _,
                  _)
                  if sourceIdentifier.identifierType == IdentifierType.SierraSystemNumber =>
                (canonicalId, sourceIdentifier)
            }

        for {
          stacksItems <- Future.sequence(
            items.map {
              case (canonicalId, sourceIdentifier) =>
                sierraService
                  .getItemStatus(sourceIdentifier)
                  .map { result =>
                    StacksItem(canonicalId, sourceIdentifier, result.right.get)
                  }
            }
          )

          stacksWork = StacksWork(workId, stacksItems)

          displayWork = DisplayStacksWork(stacksWork)
        } yield complete(displayWork)
      }
}
