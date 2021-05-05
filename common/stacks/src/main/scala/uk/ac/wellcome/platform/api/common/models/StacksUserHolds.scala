package uk.ac.wellcome.platform.api.common.models

import weco.catalogue.internal_model.identifiers.{CanonicalId, SourceIdentifier}

import java.time.Instant

case class StacksUserHolds(
  userId: String,
  holds: List[StacksHold]
)

case class StacksHold(
  id: Option[CanonicalId] = None,
  itemId: SourceIdentifier,
  pickup: StacksPickup,
  status: StacksHoldStatus
)

case class StacksHoldStatus(
  id: String,
  label: String
)

case class StacksPickup(
  location: StacksPickupLocation,
  pickUpBy: Option[Instant]
)
