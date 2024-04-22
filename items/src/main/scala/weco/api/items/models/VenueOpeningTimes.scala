package weco.api.items.models

import io.circe.generic.extras.JsonKey

// This represents opening times as we receive from the content-api
case class VenueOpeningTimes(
  @JsonKey("type") contentType: String,
  id: String,
  title: String,
  OpeningTimes: List[OpenClose]
)
case class OpenClose(
  open: String,
  close: String
)

// make that a list of VenueOpeningTimes
