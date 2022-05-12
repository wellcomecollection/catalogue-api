package weco.api.items.json

import io.circe.Json
import weco.api.stacks.models.CatalogueIdentifier
import weco.json.JsonUtil._

trait JsonOps {
  case class HasIdentifiers(
    identifiers: List[CatalogueIdentifier]
  )

  implicit class CatalogueJsonOps(json: Json) {
    def identifierType: Option[String] =
      json.identifier.map(_.identifierType.id)

    def identifier: Option[CatalogueIdentifier] =
      json.as[HasIdentifiers] match {
        case Right(HasIdentifiers(identifiers)) => identifiers.headOption
        case _ => None
      }
  }
}
