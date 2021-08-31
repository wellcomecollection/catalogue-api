package weco.api.stacks.models

import weco.catalogue.internal_model.locations.{AccessCondition, AccessMethod, PhysicalLocationType}
import weco.catalogue.source_model.sierra.rules.{SierraItemAccess, SierraPhysicalLocationType}
import weco.sierra.models.data.SierraItemData

object SierraItemDataOps {
  implicit class ItemDataOps(itemData: SierraItemData) {
    lazy val accessCondition: AccessCondition = {
      val location: Option[PhysicalLocationType] =
        itemData.fixedFields
          .get("79")
          .flatMap(_.display)
          .flatMap(
            name => SierraPhysicalLocationType.fromName(itemData.id, name)
          )

      val (accessCondition, _) = SierraItemAccess(
        location = location,
        itemData = itemData
      )

      accessCondition
    }

    lazy val allowsOnlineRequesting: Boolean =
      accessCondition.method == AccessMethod.OnlineRequest
  }
}
