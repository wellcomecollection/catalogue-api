package weco.catalogue.display_model.identifiers

import io.circe.generic.extras.JsonKey

case class DisplayIdentifier(
  identifierType: DisplayIdentifierType,
  value: String,
  @JsonKey("type") ontologyType: String = "Identifier"
)
