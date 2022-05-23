package weco.api.stacks.models

import weco.catalogue.display_model.locations.{DisplayAccessCondition, DisplayLocationType}
import weco.catalogue.source_model.sierra.rules.SierraItemAccess
import weco.sierra.models.data.SierraItemData

object SierraItemDataOps {
  implicit class ItemDataOps(itemData: SierraItemData) {
    def accessCondition(
      location: Option[DisplayLocationType]
    ): Option[DisplayAccessCondition] = {
      val (accessCondition, _) = SierraItemAccess(
        location = location,
        itemData = itemData
      )

      accessCondition
    }

    def allowsOnlineRequesting(location: Option[DisplayLocationType]): Boolean =
      accessCondition(location)
        .map(_.method)
        .contains(CatalogueAccessMethod.OnlineRequest)
  }
}
