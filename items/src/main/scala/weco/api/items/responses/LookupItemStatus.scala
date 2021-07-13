package weco.api.items.responses

import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import weco.api.items.models.DisplayItemsList
import weco.api.items.services.ItemUpdateService
import weco.api.search.rest.SingleWorkDirectives
import weco.api.stacks.services.WorkLookup
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.work.{Work, WorkState}

import scala.concurrent.Future

trait LookupItemStatus extends SingleWorkDirectives {
  import weco.catalogue.display_model.models.Implicits._

  val itemUpdateService: ItemUpdateService

  val workLookup: WorkLookup
  val index: Index

  def lookupStatus(workId: CanonicalId): Future[Route] =
    workLookup
      .byCanonicalId(workId)(index)
      .mapVisible { work: Work.Visible[WorkState.Indexed] =>
        itemUpdateService
          .updateItems(work)
          .map(DisplayItemsList(_))
          .map(complete(_))
      }
}
