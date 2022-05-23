package weco.catalogue.display_model.locations

import io.circe.generic.extras.JsonKey

case class DisplayAccessCondition(
  method: DisplayAccessMethod,
  status: Option[DisplayAccessStatus],
  terms: Option[String],
  note: Option[String],
  @JsonKey("type") ontologyType: String = "AccessCondition"
)

object DisplayAccessCondition {
  def apply(
    method: DisplayAccessMethod,
    status: DisplayAccessStatus
  ): DisplayAccessCondition =
    DisplayAccessCondition(
      method = method,
      status = Some(status),
      terms = None,
      note = None
    )

  def apply(method: DisplayAccessMethod): DisplayAccessCondition =
    DisplayAccessCondition(
      method = method,
      status = None,
      terms = None,
      note = None
    )
}
