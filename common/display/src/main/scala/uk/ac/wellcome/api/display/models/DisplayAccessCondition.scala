package uk.ac.wellcome.api.display.models

import io.circe.generic.extras.JsonKey
import io.swagger.v3.oas.annotations.media.Schema
import weco.catalogue.internal_model.locations.AccessCondition

@Schema(
  name = "AccessCondition"
)
case class DisplayAccessCondition(
  method: Option[DisplayAccessMethod],
  status: Option[DisplayAccessStatus],
  terms: Option[String],
  to: Option[String],
  note: Option[String],
  @JsonKey("type") @Schema(name = "type") ontologyType: String =
    "AccessCondition"
)

object DisplayAccessCondition {

  def apply(accessCondition: AccessCondition): DisplayAccessCondition =
    DisplayAccessCondition(
      method = accessCondition.method.map(DisplayAccessMethod.apply),
      status = accessCondition.status.map(DisplayAccessStatus.apply),
      terms = accessCondition.terms,
      to = accessCondition.to,
      note = accessCondition.note,
    )
}
