package weco.api.stacks.http

import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.Materializer
import io.circe.Encoder
import uk.ac.wellcome.json.JsonUtil._
import weco.api.stacks.models.{SierraErrorCode, SierraHoldRequest, SierraHoldsList, SierraItem}
import weco.catalogue.source_model.sierra.identifiers.{SierraItemNumber, SierraPatronNumber}

import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
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
  private implicit val umItemStub: Unmarshaller[HttpEntity, SierraItem] =
    CirceMarshalling.fromDecoder[SierraItem]

  private implicit val umErrorCode: Unmarshaller[HttpEntity, SierraErrorCode] =
    CirceMarshalling.fromDecoder[SierraErrorCode]

  def lookupItem(item: SierraItemNumber)
    : Future[Either[SierraItemLookupError, SierraItem]] =
    for {
      resp <- client.get(
        path = Path(s"v5/items/${item.withoutCheckDigit}")
      )

      result <- resp.status match {
        case StatusCodes.OK =>
          Unmarshal(resp)
            .to[SierraItem]
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

  private val dateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd")
    .withZone(ZoneId.systemDefault())

  implicit val encodeInstant: Encoder[Instant] =
    Encoder.encodeString.contramap[Instant](dateTimeFormatter.format)

  def createHold(patron: SierraPatronNumber, item: SierraItemNumber): Future[Either[SierraErrorCode, Unit]] =
    for {
      resp <- client.post(
        path = Path(s"v5/patrons/${patron.withoutCheckDigit}/holds/requests"),
        body = Some(SierraHoldRequest(item)),
      )

      result <- resp.status match {
        case StatusCodes.NoContent => Future.successful(Right(()))
        case _                     => Unmarshal(resp).to[SierraErrorCode].map(Left(_))
      }
    } yield result
}
