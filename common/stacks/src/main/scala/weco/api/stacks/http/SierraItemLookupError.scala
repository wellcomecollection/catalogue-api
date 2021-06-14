package weco.api.stacks.http

import weco.api.stacks.models.SierraErrorCode

sealed trait SierraItemLookupError

object SierraItemLookupError {
  case class ItemHasNoStatus(t: Throwable) extends SierraItemLookupError

  case object ItemNotFound extends SierraItemLookupError

  case class UnknownError(errorCode: SierraErrorCode)
    extends SierraItemLookupError
}
