package weco.catalogue.display_model.locations

import io.circe.generic.extras.JsonKey

case class DisplayLocationType(
  id: String,
  label: String,
  @JsonKey("type") ontologyType: String = "LocationType"
)
