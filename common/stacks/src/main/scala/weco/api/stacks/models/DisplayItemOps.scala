package weco.api.stacks.models

import weco.catalogue.display_model.identifiers.DisplayIdentifier
import weco.catalogue.display_model.locations.DisplayPhysicalLocation
import weco.catalogue.display_model.work.DisplayItem

trait DisplayItemOps {
  implicit class DisplayItemOps(item: DisplayItem) {

    def sourceIdentifier: Option[DisplayIdentifier] =
      item.identifiers.headOption

    /** There are two cases we care about where the data in the catalogue API
      * might be stale:
      *
      *   1) An item was on request for a user, but has been returned to the stores.
      *      Another user could now request the item, but the catalogue API will
      *      tell you it's temporarily unavailable.
      *
      *   2) An item is in the closed stores, and been requested by a user.
      *      Another user can no longer request the item, but the catalogue API will
      *      tell you it's available.
      *
      * In all other cases, we can use the access conditions in the catalogue API.
      * There may be a small delay in updating an item's information, but this is
      * relatively tolerable, and allows us to simplify the logic in this API for
      * determining item status.
      *
      * This mirrors logic in the front-end:
      * https://github.com/wellcomecollection/wellcomecollection.org/blob/fbec553332d061a6cdec5580c591ca810833e629/catalogue/webapp/components/PhysicalItems/PhysicalItems.tsx#L25-L28
      *
      */
    def isStale: Boolean = {

      // In practice we know an item only has one access condition
      val accessCondition = item.locations
        .collect { case loc: DisplayPhysicalLocation => loc }
        .flatMap(_.accessConditions)
        .headOption

      val statusId = accessCondition
        .flatMap(_.status)
        .map(_.id)

      val methodId = accessCondition.map(_.method.id)

      val isTemporarilyUnavailable = statusId.contains("temporarily-unavailable")

      val isOnlineRequest = methodId.contains("online-request")
      val hasRequestableStatus = statusId.contains("open") || statusId.contains(
        "open-with-advisory"
      ) || statusId.contains("restricted") || statusId.isEmpty

      val isRequestable = isOnlineRequest && hasRequestableStatus

      isTemporarilyUnavailable || isRequestable
    }
  }
}
