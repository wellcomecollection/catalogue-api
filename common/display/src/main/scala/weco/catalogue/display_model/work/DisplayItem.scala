package weco.catalogue.display_model.work

import io.circe.generic.extras.JsonKey
import weco.catalogue.display_model.identifiers.DisplayIdentifier
import weco.catalogue.display_model.locations.DisplayLocation

case class DisplayItem(
  id: Option[String],
  identifiers: List[DisplayIdentifier],
  title: Option[String] = None,
  note: Option[String] = None,
  locations: List[DisplayLocation] = List(),
  @JsonKey("type") ontologyType: String = "Item"
)
