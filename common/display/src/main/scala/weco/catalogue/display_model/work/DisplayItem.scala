package weco.catalogue.display_model.work

import io.circe.generic.extras.JsonKey
import weco.catalogue.display_model.identifiers.DisplayIdentifier
import weco.catalogue.display_model.locations.DisplayLocation
import java.time.ZonedDateTime

case class AvailabilitySlot(
  from: ZonedDateTime,
  to: ZonedDateTime
)
case class DisplayItem(
  id: Option[String],
  identifiers: List[DisplayIdentifier],
  title: Option[String] = None,
  note: Option[String] = None,
  locations: List[DisplayLocation] = List(),
  availableDates: Option[List[AvailabilitySlot]] = None,
  @JsonKey("type") ontologyType: String = "Item"
)
