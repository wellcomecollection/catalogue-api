package weco.catalogue.display_model.locations

import io.circe.generic.extras.JsonKey
import weco.catalogue.internal_model.locations.License

case class DisplayLicense(
  id: String,
  label: String,
  url: String,
  @JsonKey("type") ontologyType: String = "License"
)
