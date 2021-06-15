package weco.api.stacks.models

import weco.catalogue.source_model.sierra.identifiers.SierraPatronNumber

sealed trait HoldResponse

case object HoldAccepted extends HoldResponse
case object HoldRejected extends HoldResponse
case object UserAtHoldLimit extends HoldResponse
case object CannotBeRequested extends HoldResponse
case object UnknownError extends HoldResponse
case object OnHoldForAnotherUser extends HoldResponse
case class NoSuchUser(patron: SierraPatronNumber)
    extends HoldResponse
