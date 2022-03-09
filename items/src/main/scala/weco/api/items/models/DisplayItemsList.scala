package weco.api.items.models

import io.circe.generic.extras.JsonKey
import weco.catalogue.display_model.models.DisplayItem
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.work.Item

case class DisplayItemsList(
  @JsonKey("type") ontologyType: String = "ItemsList",
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
