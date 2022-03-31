package weco.api.requests

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive, PathMatcher, Route}
import weco.api.requests.models.ItemRequest
import weco.api.requests.responses.{CreateRequest, LookupPendingRequests}
import weco.api.requests.services.RequestsService
import weco.api.search.models.ApiConfig
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.http.ErrorDirectives
import weco.http.models.DisplayError
import weco.sierra.models.identifiers.SierraPatronNumber

import java.time.LocalDate
import scala.concurrent.ExecutionContext
import scala.util.{Success, Try}

class RequestsApi(
  val requestsService: RequestsService
)(
  implicit
  val ec: ExecutionContext,
  val apiConfig: ApiConfig
) extends CreateRequest
    with LookupPendingRequests {

  val routes: Route = concat(
    RequestsApi
      .withUserId("users" / Segment / "item-requests") {
        userIdentifier: SierraPatronNumber =>
          post {
            entity(as[ItemRequest]) {
              itemRequest: ItemRequest =>
                // TODO: We get the work ID as part of the item request, although right now
                // it's only for future-proofing, in case it's useful later.
                // Should we query based on the work ID?
                Try { CanonicalId(itemRequest.itemId) } match {
                  case Success(itemId) =>
                    withFuture {
                      createRequest(
                        itemId = itemId,
                        pickupDate = itemRequest.pickupDate,
                        patronNumber = userIdentifier
                      )
                    }

                  case _ =>
                    notFound(
                      s"Item not found for identifier ${itemRequest.itemId}"
                    )
                }
            }
          } ~ get {
            lookupRequests(userIdentifier)
          }
      }
  )
}

object RequestsApi extends ErrorDirectives {
  def withUserId(
    pathMatcher: PathMatcher[Tuple1[String]]
  ): Directive[Tuple1[SierraPatronNumber]] = pathPrefix(pathMatcher).flatMap {
    pathId: String =>
      optionalHeaderValueByName("X-Wellcome-Caller-ID").flatMap {
        contextId: Option[String] =>
          (pathId, contextId) match {
            case ("me", Some(userId)) => provide(SierraPatronNumber(userId))
            case ("me", None) =>
              complete(
                StatusCodes.Unauthorized -> DisplayError(
                  StatusCodes.Unauthorized,
                  "Caller attempted to operate on themselves, but request was not authorised"
                )
              )
            case (userId, _) => provide(SierraPatronNumber(userId))
          }
      }
  }
}
