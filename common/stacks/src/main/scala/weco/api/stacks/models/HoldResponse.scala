package weco.api.stacks.models

import java.time.Instant

sealed trait HoldResponse {
  val lastModified: Instant
}
case class HoldAccepted(lastModified: Instant = Instant.now())
    extends HoldResponse
case class HoldRejected(lastModified: Instant = Instant.now())
    extends HoldResponse
case class UserAtHoldLimit(lastModified: Instant = Instant.now())
    extends HoldResponse
case class CannotBeRequested(lastModified: Instant = Instant.now())
    extends HoldResponse
case class UnknownError(lastModified: Instant = Instant.now())
    extends HoldResponse
case class OnHoldForAnotherUser(lastModified: Instant = Instant.now())
  extends HoldResponse
