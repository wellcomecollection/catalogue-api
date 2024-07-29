package weco.api.items.services

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import grizzled.slf4j.Logging
import weco.http.client.{HttpClient, HttpGet}
import weco.http.json.CirceMarshalling
import weco.json.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}
import weco.api.items.models.{ContentApiVenue, ContentApiVenueResponse}

sealed trait VenueOpeningTimesLookupError {
  val title: String
}
case class VenueOpeningTimesNotFoundError(title: String)
    extends VenueOpeningTimesLookupError
case class UnknownOpeningTimesError(title: String, err: Throwable)
    extends VenueOpeningTimesLookupError

class VenuesOpeningTimesLookup(client: HttpClient with HttpGet)(
  implicit as: ActorSystem,
  ec: ExecutionContext
) extends Logging {

  implicit val um: Unmarshaller[HttpEntity, ContentApiVenueResponse] =
    CirceMarshalling.fromDecoder[ContentApiVenueResponse]

  /** Returns venue(s) that corresponds to the title(s).
    *
    */
  def byVenueName(
    venueName: String
  ): Future[
    Either[VenueOpeningTimesLookupError, List[ContentApiVenue]]
  ] = {
    val path = Path(s"venues")
    // on-site and deepstore items are both viewed in the library, so we fetch it by default
    val params = Map("title" -> s"library,$venueName")

    val httpResult = for {
      response <- client.get(path = path, params = params)

      result <- matchResponseToResult(response, path, params, venueName)
    } yield result

    httpResult.recover {
      case e => Left(UnknownOpeningTimesError(venueName, e))
    }
  }

  private def matchResponseToResult(
    response: HttpResponse,
    path: Path,
    params: Map[String, String],
    venueName: String
  ): Future[Either[VenueOpeningTimesLookupError, List[ContentApiVenue]]] =
    response.status match {
      case StatusCodes.OK =>
        info(s"OK for GET to $path with $params")
        Unmarshal(response.entity).to[ContentApiVenueResponse].map { response =>
          Right(response.results)
        }

      case StatusCodes.NotFound =>
        error(s"Not Found for GET to $path with $params")
        Future(Left(VenueOpeningTimesNotFoundError(venueName)))

      case status =>
        val err = new Throwable(s"$status from the content API")
        error(
          s"Unexpected status from GET to $path with $params: $status",
          err
        )
        Future(Left(UnknownOpeningTimesError(venueName, err)))
    }
}
