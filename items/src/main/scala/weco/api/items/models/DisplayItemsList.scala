package weco.api.items.models

import io.circe.generic.extras.JsonKey
import io.swagger.v3.oas.annotations.media.Schema
import weco.catalogue.display_model.models.DisplayItem
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.work.Item

@Schema(
  name = "ItemList",
  description = "A list of items."
)
case class DisplayItemsList(
  @JsonKey("type") @Schema(name = "type") ontologyType: String = "ItemsList",
  totalResults: Int,
  results: Seq[DisplayItem]
)

object DisplayItemsList {

  def apply(items: Seq[Item[IdState.Minted]]): DisplayItemsList = {
    val displayItems: Seq[DisplayItem] =
      items.map(item => DisplayItem(item = item, includesIdentifiers = true))

    DisplayItemsList(
      totalResults = displayItems.length,
      results = displayItems
    )
  }
}
