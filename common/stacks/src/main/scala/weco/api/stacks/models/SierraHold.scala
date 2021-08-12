package weco.api.stacks.models

import weco.sierra.models.fields.SierraLocation

import java.net.URI
import java.time.Instant

// This represents a Sierra Hold object, as described in the Sierra docs:
// https://techdocs.iii.com/sierraapi/Content/zReference/objects/holdObjectDescription.htm

case class SierraHoldStatus(
  code: String,
  name: String
)

case class SierraHold(
  id: URI,
  record: URI,
  pickupLocation: SierraLocation,
  pickupByDate: Option[Instant],
  status: SierraHoldStatus
)
