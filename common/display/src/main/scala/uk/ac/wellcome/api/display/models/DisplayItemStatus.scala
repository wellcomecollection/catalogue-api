package uk.ac.wellcome.api.display.models

import io.circe.generic.extras.JsonKey
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  name = "Item status"
)
case class DisplayItemStatus(
  @Schema id: String,
  @Schema label: String,
  @JsonKey("type") @Schema(name = "type") ontologyType: String = "ItemStatus"
)
