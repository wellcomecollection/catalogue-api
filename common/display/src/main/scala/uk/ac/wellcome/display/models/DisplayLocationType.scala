package uk.ac.wellcome.display.models

import io.circe.generic.extras.JsonKey
import io.swagger.v3.oas.annotations.media.Schema
import weco.catalogue.internal_model.locations.LocationType

@Schema(
  name = "LocationType"
)
case class DisplayLocationType(
  @Schema id: String,
  @Schema label: String,
  @JsonKey("type") @Schema(name = "type") ontologyType: String = "LocationType"
)

object DisplayLocationType {
  def apply(locationType: LocationType): DisplayLocationType =
    DisplayLocationType(id = locationType.id, label = locationType.label)
}
