package weco.api.items.models

import io.circe.generic.extras.JsonKey

// This represents opening times as we receive from the content-api
case class ContentApiVenueResponse(
  @JsonKey("type") responseType: String,
  results: List[VenueOpeningTimes]
)

case class VenueOpeningTimes(
  @JsonKey("type") contentType: String,
  id: String,
  title: String,
  @JsonKey("nextOpeningDates") openingTimes: List[OpenClose]
)
case class OpenClose(
  open: String,
  close: String
)
