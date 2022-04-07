package weco.api.items.services

import weco.catalogue.display_model.models.DisplayItem
import weco.catalogue.internal_model.identifiers.IdentifierType

import scala.concurrent.Future

trait ItemUpdater {
  val identifierType: IdentifierType
  def updateItems(items: Seq[DisplayItem]): Future[Seq[DisplayItem]]
}
