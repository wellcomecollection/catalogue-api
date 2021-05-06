package uk.ac.wellcome.platform.api.common.services.source

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import uk.ac.wellcome.platform.api.common.models.Identifier
import uk.ac.wellcome.platform.api.http.AkkaClientGet
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.Future

trait CatalogueSource {
  import CatalogueSource._

  def getSearchStub(canonicalId: CanonicalId): Future[SearchStub]
  def getSearchStub(identifier: Identifier[_]): Future[SearchStub]
}

object CatalogueSource {
  case class TypeStub(
    id: String,
    label: String
  )

  case class IdentifiersStub(
    identifierType: TypeStub,
    value: String
  )

  case class ItemStub(
    id: Option[String],
    identifiers: Option[List[IdentifiersStub]]
  )

  case class WorkStub(
    id: String,
    items: List[ItemStub]
  )

  case class SearchStub(
    totalResults: Int,
    results: List[WorkStub]
  )
}

class AkkaCatalogueSource(
  val baseUri: Uri = Uri(
    "https://api.wellcomecollection.org/catalogue/v2"
  )
)(
  implicit
  val system: ActorSystem,
) extends CatalogueSource
    with AkkaClientGet {

  import CatalogueSource._
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  // See https://developers.wellcomecollection.org/catalogue/v2/works/getworks
  def getSearchStub(identifier: Identifier[_]): Future[SearchStub] =
    get[SearchStub](
      path = Path("works"),
      params = Map(
        ("include", "items,identifiers"),
        ("query", identifier.value.toString)
      )
    ) map {
      case SuccessResponse(Some(workStub)) => workStub
      case _                               => throw new Exception("Failed to make catalogue search!")
    }

  // See https://developers.wellcomecollection.org/catalogue/v2/works/getworks
  def getSearchStub(canonicalId: CanonicalId): Future[SearchStub] =
    get[SearchStub](
      path = Path("works"),
      params = Map(
        ("include", "items,identifiers"),
        ("query", canonicalId.toString)
      )
    ) map {
      case SuccessResponse(Some(workStub)) => workStub
      case _                               => throw new Exception("Failed to make catalogue search!")
    }
}
