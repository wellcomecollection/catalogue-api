package weco.api.stacks.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import uk.ac.wellcome.platform.api.common.services.source.SierraSource.{SierraErrorCode, SierraItemStub}
import weco.catalogue.source_model.sierra.identifiers.SierraItemNumber

import scala.concurrent.{ExecutionContext, Future}

class SierraSource(client: HttpClient)(implicit ec: ExecutionContext, mat: Materializer) {
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  def lookupItem(item: SierraItemNumber): Future[Either[SierraErrorCode, SierraItemStub]] =
    for {
      resp <- client.get(
        path = Path(s"v5/items/${item.withoutCheckDigit}")
      )

      result <- resp.status match {
        case StatusCodes.OK => Unmarshal(resp).to[SierraItemStub].map(Right(_))
        case _              => Unmarshal(resp).to[SierraErrorCode].map(Left(_))
      }
    } yield result
}
