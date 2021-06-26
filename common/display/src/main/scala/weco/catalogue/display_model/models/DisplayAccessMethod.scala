package weco.catalogue.display_model.models

import io.circe.generic.extras.JsonKey
import io.swagger.v3.oas.annotations.media.Schema
import weco.catalogue.internal_model.locations.AccessMethod

@Schema(
  name = "AccessMethod"
)
case class DisplayAccessMethod(
  id: String,
  label: String,
  @JsonKey("type") @Schema(name = "type") ontologyType: String = "AccessMethod"
)

object DisplayAccessMethod {
  def apply(accessMethod: AccessMethod): DisplayAccessMethod =
    accessMethod match {
      case AccessMethod.OnlineRequest =>
        DisplayAccessMethod("online-request", "Online request")
      case AccessMethod.ManualRequest =>
        DisplayAccessMethod("manual-request", "Manual request")
      case AccessMethod.NotRequestable =>
        DisplayAccessMethod("not-requestable", "Not requestable")
      case AccessMethod.ViewOnline =>
        DisplayAccessMethod("view-online", "View online")
      case AccessMethod.OpenShelves =>
        DisplayAccessMethod("open-shelves", "Open shelves")
    }
}
