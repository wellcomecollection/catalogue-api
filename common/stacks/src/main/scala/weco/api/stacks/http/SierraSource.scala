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

import akka.http.scaladsl.model

import scala.concurrent.{ExecutionContext, Future}

case class SierraItemDataEntries(
  total: Int,
  start: Int,
  entries: Seq[SierraItemData]
)

class SierraSource(client: HttpClient with HttpGet with HttpPost)(
  implicit
  ec: ExecutionContext,
  mat: Materializer
) {
  private implicit val umItemStub: Unmarshaller[HttpEntity, SierraItemData] =
    CirceMarshalling.fromDecoder[SierraItemData]

  private implicit val umItemEntriesStub
    : Unmarshaller[HttpEntity, SierraItemDataEntries] =
    CirceMarshalling.fromDecoder[SierraItemDataEntries]

  private implicit val umErrorCode: Unmarshaller[HttpEntity, SierraErrorCode] =
    CirceMarshalling.fromDecoder[SierraErrorCode]

  type Unmarshalled[T] =
    Future[Either[SierraItemLookupError.ItemHasNoStatus, T]]

  def unmarshalOKResponse[T](
    response: model.HttpResponse
  )(implicit um: Unmarshaller[model.HttpResponse, T]): Unmarshalled[T] =
    Unmarshal(response)
      .to[T]
      .map(Right(_))
      .recover {
        case t: Throwable =>
          Left(SierraItemLookupError.ItemHasNoStatus(t))
      }

  /** Returns data for a list of items
    *
    * Note: if some of the IDs requested do not exist, they will
    * not appear in the results list. It's up to the consumer to
    * identify that and react to it as required.
    */
  def lookupItemEntries(
    items: Seq[SierraItemNumber]
  ): Future[Either[SierraItemLookupError, SierraItemDataEntries]] = {

    val idList = items
      .map(_.withoutCheckDigit)
      .mkString(",")

    for {
      response: model.HttpResponse <- client.get(
        path = Path("v5/items"),
        params = Map(
          "id" -> idList,
          "fields" -> "deleted,fixedFields,holdCount,suppressed"
        )
      )

      result <- response.status match {
        case StatusCodes.OK =>
          unmarshalOKResponse[SierraItemDataEntries](response)

        case _ =>
          Unmarshal(response)
            .to[SierraErrorCode]
            .map(err => Left(SierraItemLookupError.UnknownError(err)))
      }

    } yield result
  }

  /** Returns data for a single item
    */
  def lookupItem(
    item: SierraItemNumber
  ): Future[Either[SierraItemLookupError, SierraItemData]] =
    for {
      response <- client.get(
        path = Path(s"v5/items/${item.withoutCheckDigit}"),
        params = Map("fields" -> "deleted,fixedFields,holdCount,suppressed")
      )

      result <- response.status match {
        case StatusCodes.OK =>
          unmarshalOKResponse[SierraItemData](response)
        case StatusCodes.NotFound =>
          Future.successful(Left(SierraItemLookupError.ItemNotFound))

        case _ =>
          Unmarshal(response)
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
  def listHolds(
    patron: SierraPatronNumber
  ): Future[Either[SierraErrorCode, SierraHoldsList]] =
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
    item: SierraItemNumber
  ): Future[Either[SierraErrorCode, Unit]] =
    for {
      resp <- client.post(
        path = Path(s"v5/patrons/${patron.withoutCheckDigit}/holds/requests"),
        body = Some(SierraHoldRequest(item))
      )

      result <- resp.status match {
        case StatusCodes.NoContent => Future.successful(Right(()))
        case _                     => Unmarshal(resp).to[SierraErrorCode].map(Left(_))
      }
    } yield result
}
