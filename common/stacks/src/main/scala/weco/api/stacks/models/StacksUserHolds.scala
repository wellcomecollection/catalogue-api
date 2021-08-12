package weco.api.stacks.models

import weco.catalogue.internal_model.identifiers.{CanonicalId, IdState, SourceIdentifier}
import java.time.Instant

import weco.catalogue.internal_model.work.Item

case class StacksUserHolds(
  userId: String,
  holds: List[StacksHold]
)

case class StacksHold(
  sourceIdentifier: SourceIdentifier,
  pickup: StacksPickup,
  status: StacksHoldStatus,
  item: Option[Item[IdState.Identified]] = None
)

case class StacksHoldStatus(
  id: String,
  label: String
)

case class StacksPickup(
  location: StacksPickupLocation,
  pickUpBy: Option[Instant]
)
