package weco.api.stacks.http

import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.Materializer
import io.circe.Encoder
import weco.json.JsonUtil._
import weco.api.stacks.models.{
  SierraErrorCode,
  SierraHoldRequest,
  SierraHoldsList
}
import weco.catalogue.source_model.sierra.SierraItemData
import weco.catalogue.source_model.sierra.identifiers.{
  SierraItemNumber,
  SierraPatronNumber
}
import weco.http.client.{HttpClient, HttpGet, HttpPost}
import weco.http.json.CirceMarshalling

import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

class SierraSource(client: HttpClient with HttpGet with HttpPost)(
  implicit
  ec: ExecutionContext,
  mat: Materializer) {
  private implicit val umItemStub: Unmarshaller[HttpEntity, SierraItemData] =
    CirceMarshalling.fromDecoder[SierraItemData]

  private implicit val umErrorCode: Unmarshaller[HttpEntity, SierraErrorCode] =
    CirceMarshalling.fromDecoder[SierraErrorCode]

  def lookupItem(item: SierraItemNumber)
    : Future[Either[SierraItemLookupError, SierraItemData]] =
    for {
      resp <- client.get(
        path = Path(s"v5/items/${item.withoutCheckDigit}"),
        params = Map("fields" -> "deleted,fixedFields,holdCount,suppressed")
      )

      result <- resp.status match {
        case StatusCodes.OK =>
          Unmarshal(resp)
            .to[SierraItemData]
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

  /** Returns a list of holds for this user.
    *
    * Note: do not rely on this method to prove the existence of a user.
    * In particular, the Sierra API will return an empty list of holds if you
    * query this API for a patron ID that doesn't exist.
    *
    */
  def listHolds(patron: SierraPatronNumber)
    : Future[Either[SierraErrorCode, SierraHoldsList]] =
    for {
      resp <- client.get(
        path = Path(s"v5/patrons/${patron.withoutCheckDigit}/holds"),
        params = Map("limit" -> "100", "offset" -> "0")
      )

      result <- resp.status match {
        case StatusCodes.OK => Unmarshal(resp).to[SierraHoldsList].map(Right(_))
        case _              => Unmarshal(resp).to[SierraErrorCode].map(Left(_))
      }
    } yield result

  private val dateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd")
    .withZone(ZoneId.systemDefault())

  implicit val encodeInstant: Encoder[Instant] =
    Encoder.encodeString.contramap[Instant](dateTimeFormatter.format)

  def createHold(
    patron: SierraPatronNumber,
    item: SierraItemNumber): Future[Either[SierraErrorCode, Unit]] =
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
