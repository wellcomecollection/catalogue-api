package weco.api.stacks.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import uk.ac.wellcome.platform.api.common.services.source.SierraSource.{
  SierraErrorCode,
  SierraItemStub
}
import weco.catalogue.source_model.sierra.identifiers.SierraItemNumber

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
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

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
}
