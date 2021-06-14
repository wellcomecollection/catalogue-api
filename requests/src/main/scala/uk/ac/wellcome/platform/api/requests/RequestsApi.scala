package uk.ac.wellcome.platform.api.requests

import akka.http.scaladsl.server.Route
import uk.ac.wellcome.platform.api.common.services.SierraService
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.platform.api.requests.models.ItemRequest
import weco.api.requests.responses.{CreateRequest, LookupPendingRequests}
import weco.api.stacks.services.ItemLookup
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.source_model.sierra.identifiers.SierraPatronNumber

import scala.concurrent.ExecutionContext
import scala.util.{Success, Try}

class RequestsApi(
  val sierraService: SierraService,
  val itemLookup: ItemLookup
)(
  implicit
  val ec: ExecutionContext,
  val apiConfig: ApiConfig
) extends CreateRequest with LookupPendingRequests {
  val routes: Route = concat(
    pathPrefix("users" / Segment / "item-requests") { userId: String =>
      val userIdentifier = SierraPatronNumber(userId)

      post {
        entity(as[ItemRequest]) {
          itemRequest: ItemRequest =>
            // TODO: We get the work ID as part of the item request, although right now
            // it's only for future-proofing, in case it's useful later.
            // Should we query based on the work ID?
            Try { CanonicalId(itemRequest.itemId) } match {
              case Success(itemId) =>
                withFuture {
                  createRequest(itemId = itemId, patronNumber = userIdentifier)
                }

              case _ =>
                notFound(s"Item not found for identifier ${itemRequest.itemId}")
            }
        }
      } ~ get {
        lookupRequests(userIdentifier)
      }
    }
  )
}
