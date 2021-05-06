package weco.api.stacks.models

import java.time.Instant

sealed trait HoldResponse {
  val lastModified: Instant
}
case class HoldAccepted(lastModified: Instant = Instant.now())
  extends HoldResponse
case class HoldRejected(lastModified: Instant = Instant.now())
  extends HoldResponse
