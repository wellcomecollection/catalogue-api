package weco.api.items.models

import io.circe.generic.extras.JsonKey
import weco.catalogue.display_model.work.DisplayItem

case class DisplayItemsList(
  @JsonKey("type") ontologyType: String = "ItemsList",
  totalResults: Int,
  results: Seq[DisplayItem]
)
