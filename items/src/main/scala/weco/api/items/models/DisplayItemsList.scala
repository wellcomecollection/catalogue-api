package weco.api.items.models

import io.circe.Json
import io.circe.generic.extras.JsonKey

case class DisplayItemsList(
  @JsonKey("type") ontologyType: String = "ItemsList",
  totalResults: Int,
  results: Seq[Json]
)
