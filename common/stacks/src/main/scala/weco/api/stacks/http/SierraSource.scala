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
import weco.http.client.{HttpClient, HttpGet, HttpPost}
import weco.http.json.CirceMarshalling
import weco.sierra.models.data.SierraItemData
import weco.sierra.models.identifiers.{SierraItemNumber, SierraPatronNumber}
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter

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
  private implicit val umItemEntriesStub
    : Unmarshaller[HttpEntity, SierraItemDataEntries] =
    CirceMarshalling.fromDecoder[SierraItemDataEntries]

  private implicit val umErrorCode: Unmarshaller[HttpEntity, SierraErrorCode] =
    CirceMarshalling.fromDecoder[SierraErrorCode]

  /** Returns data for a list of items
    */
  def lookupItemEntries(
    itemNumbers: Seq[SierraItemNumber]
  ): Future[Either[SierraItemLookupError, SierraItemDataEntries]] = {

    val idList = itemNumbers
      .map(_.withoutCheckDigit)
      .mkString(",")

    for {
      response <- client.get(
        path = Path("v5/items"),
        params = Map(
          "id" -> idList,
          "fields" -> "deleted,fixedFields,holdCount,suppressed"
        )
      )

      result <- response.status match {
        case StatusCodes.OK =>
          Unmarshal(response).to[SierraItemDataEntries].map { itemDataEntries =>
            // There are a number of edge cases ignored here e.g.
            // - there are more items returned than requested for this query
            // - there are different items ids returned than requested for this query
            // These cases are far less likely than requesting missing items which is
            // dealt with here, so we ignore them for simplicity.
            val foundItemNumbers = itemDataEntries.entries.map(_.id)

            if (itemDataEntries.entries.size < itemNumbers.size) {
              Left(
                SierraItemLookupError.MissingItems(
                  missingItems =
                    itemNumbers.filterNot(foundItemNumbers.contains(_)),
                  itemsReturned = itemDataEntries.entries
                )
              )
            } else {
              Right(itemDataEntries)
            }
          }

        // When none of the item ids requested exist, sierra will 404
        case StatusCodes.NotFound =>
          Future.successful(
            Left(
              SierraItemLookupError.MissingItems(
                missingItems = itemNumbers,
                itemsReturned = Seq.empty
              )
            )
          )

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
    lookupItemEntries(Seq(item)).map {
      case Right(itemDataEntries) =>
        Right(itemDataEntries.entries.head)
      case Left(SierraItemLookupError.MissingItems(_, _)) =>
        Left(SierraItemLookupError.ItemNotFound)
      case Left(value) => Left(value)
    }

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
