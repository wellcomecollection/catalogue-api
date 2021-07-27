package weco.api.stacks.http

import weco.api.stacks.models.SierraErrorCode
import weco.catalogue.source_model.sierra.SierraItemData
import weco.catalogue.source_model.sierra.identifiers.SierraItemNumber

sealed trait SierraItemLookupError

object SierraItemLookupError {
  case class ItemHasNoStatus(t: Throwable) extends SierraItemLookupError

  case object ItemNotFound extends SierraItemLookupError

  case class MissingItems(
    missingItems: Seq[SierraItemNumber],
    itemsReturned: Seq[SierraItemData]
  ) extends SierraItemLookupError

  case class UnknownError(errorCode: SierraErrorCode)
      extends SierraItemLookupError
}
