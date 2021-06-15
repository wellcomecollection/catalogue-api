package weco.api.stacks.models

import weco.catalogue.source_model.sierra.identifiers.SierraPatronNumber

sealed trait HoldResponse

sealed trait HoldAccepted extends HoldResponse

object HoldAccepted {
  case object HoldCreated extends HoldAccepted
  case object HoldAlreadyExists extends HoldAccepted
}

sealed trait HoldRejected extends HoldResponse

object HoldRejected {
  case object ItemCannotBeRequested extends HoldRejected
  case object ItemIsOnHoldForAnotherUser extends HoldRejected
  case object UserIsAtHoldLimit extends HoldRejected
  case class UserDoesNotExist(patron: SierraPatronNumber) extends HoldRejected
  case object UnknownReason extends HoldRejected
}
