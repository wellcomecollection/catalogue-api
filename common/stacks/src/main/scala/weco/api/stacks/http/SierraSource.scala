package weco.api.stacks.http

import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.Materializer
import uk.ac.wellcome.platform.api.common.services.source.SierraSource.{
  SierraErrorCode,
  SierraItemStub
}
import uk.ac.wellcome.json.JsonUtil._
import weco.api.stacks.models.SierraHoldsList
import weco.catalogue.source_model.sierra.identifiers.{
  SierraItemNumber,
  SierraPatronNumber
}

import scala.concurrent.{ExecutionContext, Future}

sealed trait SierraItemLookupError

object SierraItemLookupError {
  case class ItemHasNoStatus(t: Throwable) extends SierraItemLookupError

  case object ItemNotFound extends SierraItemLookupError

  case class UnknownError(errorCode: SierraErrorCode)
      extends SierraItemLookupError
}

class SierraSource(client: HttpClient)(implicit ec: ExecutionContext,
                                       mat: Materializer) {
  private implicit val umItemStub: Unmarshaller[HttpEntity, SierraItemStub] =
    CirceMarshalling.fromDecoder[SierraItemStub]

  private implicit val umErrorCode: Unmarshaller[HttpEntity, SierraErrorCode] =
    CirceMarshalling.fromDecoder[SierraErrorCode]

  def lookupItem(item: SierraItemNumber)
    : Future[Either[SierraItemLookupError, SierraItemStub]] =
    for {
      resp <- client.get(
        path = Path(s"v5/items/${item.withoutCheckDigit}")
      )

      result <- resp.status match {
        case StatusCodes.OK =>
          Unmarshal(resp)
            .to[SierraItemStub]
            .map(Right(_))
            .recover {
              case t: Throwable =>
                Left(SierraItemLookupError.ItemHasNoStatus(t))
            }

        case StatusCodes.NotFound =>
          Future.successful(Left(SierraItemLookupError.ItemNotFound))

        case _ =>
          Unmarshal(resp)
            .to[SierraErrorCode]
            .map(err => Left(SierraItemLookupError.UnknownError(err)))
      }
    } yield result

  private implicit val umHoldsList: Unmarshaller[HttpEntity, SierraHoldsList] =
    CirceMarshalling.fromDecoder[SierraHoldsList]

  def listHolds(patron: SierraPatronNumber): Future[Either[SierraErrorCode, SierraHoldsList]] =
    for {
      resp <- client.get(
        path = Path(s"v5/patrons/${patron.withoutCheckDigit}/holds"),
        params = Map("limit" -> "100", "offset" -> "0")
      )

      result <- resp.status match {
        case StatusCodes.OK => Unmarshal(resp).to[SierraHoldsList].map(Right(_))
        case _ => Unmarshal(resp).to[SierraErrorCode].map(Left(_))
      }
    } yield result
}
