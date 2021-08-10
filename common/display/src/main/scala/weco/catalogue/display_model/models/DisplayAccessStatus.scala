package weco.catalogue.display_model.models

import io.circe.generic.extras.JsonKey
import io.swagger.v3.oas.annotations.media.Schema
import weco.catalogue.internal_model.locations.AccessStatus

@Schema(
  name = "AccessStatus"
)
case class DisplayAccessStatus(
  id: String,
  label: String,
  @JsonKey("type") @Schema(name = "type") ontologyType: String = "AccessStatus"
)

object DisplayAccessStatus {
  def apply(accessStatus: AccessStatus): DisplayAccessStatus =
    DisplayAccessStatus(
      id = accessStatus.id,
      label = accessStatus.label
    )
}
