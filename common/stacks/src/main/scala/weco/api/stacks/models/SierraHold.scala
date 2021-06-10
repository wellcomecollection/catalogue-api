package weco.api.stacks.models

import weco.catalogue.source_model.sierra.source.SierraSourceLocation

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
  pickupLocation: SierraSourceLocation,
  pickupByDate: Option[Instant],
  status: SierraHoldStatus
)
