package weco.api.items.services

import weco.catalogue.internal_model.identifiers.{IdState, IdentifierType}
import weco.catalogue.internal_model.work.Item

import scala.concurrent.Future

trait ItemUpdater {
  val identifierType: IdentifierType
  def updateItems(
    items: Seq[Item[IdState.Identified]]
  ): Future[Seq[Item[IdState.Identified]]]
}
