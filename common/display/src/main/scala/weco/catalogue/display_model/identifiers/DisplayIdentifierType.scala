package weco.catalogue.display_model.identifiers

import io.circe.generic.extras.JsonKey

case class DisplayIdentifierType(
  id: String,
  label: String,
  @JsonKey("type") ontologyType: String = "IdentifierType"
)
