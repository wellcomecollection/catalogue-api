package uk.ac.wellcome.platform.api.common.services.source

import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import io.circe.{Encoder, Printer}
import uk.ac.wellcome.json.JsonUtil._
import weco.api.stacks.http.{
  AkkaClientGet,
  AkkaClientPost,
  AkkaClientTokenExchange
}
import weco.catalogue.source_model.sierra.identifiers.{
  SierraItemNumber,
  SierraPatronNumber
}
import weco.catalogue.source_model.sierra.source.SierraSourceLocation

import java.net.URI
import scala.concurrent.Future

trait SierraSource {

  import SierraSource._

  def getSierraItemStub(id: SierraItemNumber): Future[SierraItemStub]
  def getSierraUserHoldsStub(
    patronNumber: SierraPatronNumber
  ): Future[SierraUserHoldsStub]
  def postHold(
    patronNumber: SierraPatronNumber,
    itemNumber: SierraItemNumber
  ): Future[PostHoldResult]
}

object SierraSource {
  type PostHoldResult = Either[SierraErrorCode, Unit]

  case class SierraErrorCode(
    code: Int,
    specificCode: Int,
    httpStatus: Int,
    name: String,
    description: Option[String]
  )
  case class SierraUserHoldsStatusStub(
    code: String,
    name: String
  )
  case class SierraUserHoldsEntryStub(
    id: URI,
    record: URI,
    pickupLocation: SierraSourceLocation,
    pickupByDate: Option[Instant],
    status: SierraUserHoldsStatusStub
  )
  case class SierraUserHoldsStub(
    total: Long,
    entries: List[SierraUserHoldsEntryStub]
  )
  case class SierraItemStatusStub(
    code: String,
    display: String
  )
  case class SierraItemStub(
    id: SierraItemNumber,
    status: SierraItemStatusStub
  )
  case class SierraHoldRequestPostBody(
    recordType: String,
    recordNumber: Long,
    pickupLocation: String
  )
}

class AkkaSierraSource(
  val baseUri: Uri = Uri(
    "https://libsys.wellcomelibrary.org/iii/sierra-api"
  ),
  credentials: BasicHttpCredentials
)(
  implicit
  val system: ActorSystem
) extends SierraSource
    with AkkaClientGet
    with AkkaClientPost
    with AkkaClientTokenExchange {

  import io.circe.generic.auto._
  import SierraSource._

  // Sierra will not tolerate null values in optional fields
  implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

  override val tokenPath = Path("v5/token")

  // See https://sandbox.iii.com/iii/sierra-api/swagger/index.html#!/items
  def getSierraItemStub(itemNumber: SierraItemNumber): Future[SierraItemStub] =
    for {
      token <- getToken(credentials)
      item <- get[SierraItemStub](
        path = Path(s"v5/items/${itemNumber.withoutCheckDigit}"),
        headers = List(Authorization(token))
      )
    } yield
      item match {
        case SuccessResponse(Some(holds)) => holds
        case _                            => throw new Exception(s"Failed to get item!")
      }

  // See https://sandbox.iii.com/iii/sierra-api/swagger/index.html#!/patrons
  def getSierraUserHoldsStub(
    patronNumber: SierraPatronNumber
  ): Future[SierraUserHoldsStub] =
    for {
      token <- getToken(credentials)
      response <- get[SierraUserHoldsStub](
        path = Path(s"v5/patrons/${patronNumber.withoutCheckDigit}/holds"),
        params = Map(
          ("limit", "100"),
          ("offset", "0")
        ),
        headers = List(Authorization(token))
      )
    } yield
      response match {
        case SuccessResponse(Some(holds)) => holds
        case _                            => throw new Exception(s"Failed to get user holds!")
      }

  private val dateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd")
    .withZone(ZoneId.systemDefault())

  implicit val encodeInstant: Encoder[Instant] =
    Encoder.encodeString.contramap[Instant](dateTimeFormatter.format)

  // See https://sandbox.iii.com/iii/sierra-api/swagger/index.html#!/patrons
  def postHold(
    patronNumber: SierraPatronNumber,
    itemNumber: SierraItemNumber
  ): Future[PostHoldResult] =
    for {
      token <- getToken(credentials)
      response <- post[SierraHoldRequestPostBody, SierraErrorCode](
        path =
          Path(s"v5/patrons/${patronNumber.withoutCheckDigit}/holds/requests"),
        body = Some(
          SierraHoldRequestPostBody(
            recordType = "i",
            recordNumber = itemNumber.withoutCheckDigit.toLong,
            // This field is required non-empty by the Sierra API - but has no effect
            // TODO: Is it really?
            pickupLocation = "unspecified"
          )
        ),
        headers = List(Authorization(token))
      )
    } yield
      response match {
        case SuccessResponse(_)                     => Right(())
        case FailureResponse(Some(sierraErrorCode)) => Left(sierraErrorCode)
        case _ =>
          throw new Exception(
            s"Failed to make hold!"
          )
      }
}
