package weco.api.items.services

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import grizzled.slf4j.Logging
import weco.http.client.{HttpClient, HttpGet}
import weco.http.json.CirceMarshalling
import weco.json.JsonUtil._
import scala.concurrent.{ExecutionContext, Future}
import weco.api.items.models.VenueOpeningTimes

sealed trait VenueOpeningTimesLookupError {
  val title: String
}
case class VenueOpeningTimesNotFoundError(title: String)
    extends VenueOpeningTimesLookupError
case class UnknownOpeningTimesError(title: String, err: Throwable)
    extends VenueOpeningTimesLookupError

class VenueOpeningTimesLookup(client: HttpClient with HttpGet)(
  implicit as: ActorSystem,
  ec: ExecutionContext
) extends Logging {

  implicit val um: Unmarshaller[HttpEntity, VenueOpeningTimes] =
    CirceMarshalling.fromDecoder[VenueOpeningTimes]

  /** Returns the Work that corresponds to this canonical ID.
    *
    */
  def byVenueName(
    title: Option[String]
  ): Future[Either[VenueOpeningTimesLookupError, VenueOpeningTimes]] =
    title match {
      case None =>
        throw new Throwable("A venue name was not supplied")
      case Some(title) => {
        val path = Path(s"venues")
        val params = Map("title" -> title)

        val httpResult = for {
          response <- client.get(path = path, params = params)

          result <- response.status match {
            case StatusCodes.OK =>
              info(s"OK for GET to $path with $params")
              Unmarshal(response.entity).to[VenueOpeningTimes].map {
                Right(_)
              }

            case StatusCodes.NotFound =>
              info(s"Not Found for GET to $path with $params")
              Future(Left(VenueOpeningTimesNotFoundError(title)))

            case status =>
              val err = new Throwable(s"$status from the content API")
              error(
                s"Unexpected status from GET to $path with $params: $status",
                err
              )
              Future(Left(UnknownOpeningTimesError(title, err)))
          }
        } yield result

        httpResult.recover {
          case e => Left(UnknownOpeningTimesError(title, e))
        }
      }
    }
}
