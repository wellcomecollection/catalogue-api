package weco.api.items.services

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import grizzled.slf4j.Logging
import weco.api.stacks.models.CatalogueWork
import weco.http.client.{HttpClient, HttpGet}
import weco.http.json.CirceMarshalling
import weco.catalogue.display_model.Implicits._
import weco.json.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

sealed trait WorkLookupError {
  val id: String
}

case class WorkNotFoundError(id: String) extends WorkLookupError
case class WorkGoneError(id: String) extends WorkLookupError
case class UnknownWorkError(id: String, err: Throwable)
    extends WorkLookupError

class WorkLookup(client: HttpClient with HttpGet)(
  implicit as: ActorSystem,
  ec: ExecutionContext
) extends Logging {

  implicit val um: Unmarshaller[HttpEntity, CatalogueWork] =
    CirceMarshalling.fromDecoder[CatalogueWork]

  /** Returns the Work that corresponds to this canonical ID.
    *
    */
  def byCanonicalId(
    id: String
  ): Future[Either[WorkLookupError, CatalogueWork]] = {
    val path = Path(s"works/$id")
    val params = Map("include" -> "identifiers,items")

    val httpResult = for {
      response <- client.get(path = path, params = params)

      result <- response.status match {
        case StatusCodes.OK =>
          info(s"OK for GET to $path with $params")
          Unmarshal(response.entity).to[CatalogueWork].map { Right(_) }

        case StatusCodes.NotFound =>
          info(s"Not Found for GET to $path with $params")
          Future(Left(WorkNotFoundError(id)))

        case StatusCodes.Gone =>
          info(s"Gone for GET to $path with $params")
          Future(Left(WorkGoneError(id)))

        case status =>
          val err = new Throwable(s"$status from the catalogue API")
          error(
            s"Unexpected status from GET to $path with $params: $status",
            err
          )
          Future(Left(UnknownWorkError(id, err)))
      }
    } yield result

    httpResult.recover { case e => Left(UnknownWorkError(id, e)) }
  }
}
