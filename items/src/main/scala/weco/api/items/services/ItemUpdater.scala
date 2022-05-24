package weco.api.items.services

import weco.catalogue.display_model.identifiers.DisplayIdentifierType
import weco.catalogue.display_model.work.DisplayItem

import scala.concurrent.Future

trait ItemUpdater {
  val identifierType: DisplayIdentifierType
  def updateItems(items: Seq[DisplayItem]): Future[Seq[DisplayItem]]
}
