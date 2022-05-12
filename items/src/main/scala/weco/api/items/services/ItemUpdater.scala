package weco.api.items.services

import io.circe.Json
import weco.api.stacks.models.CatalogueIdentifierType

import scala.concurrent.Future

trait ItemUpdater {
  val identifierType: CatalogueIdentifierType
  def updateItems(items: Seq[Json]): Future[Seq[Json]]
}
