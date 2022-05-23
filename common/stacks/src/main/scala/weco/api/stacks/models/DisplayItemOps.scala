package weco.api.stacks.models

import weco.catalogue.display_model.identifiers.DisplayIdentifier
import weco.catalogue.display_model.locations.{
  DisplayAccessCondition,
  DisplayLocationType,
  DisplayPhysicalLocation
}
import weco.catalogue.display_model.work.DisplayItem

trait DisplayItemOps {
  implicit class DisplayItemOps(item: DisplayItem) {

    def sourceIdentifier: Option[DisplayIdentifier] =
      item.identifiers.headOption

    /** Get the physical location for an item.
      *
      * In practice we know an item will only have at most one physical location.
      */
    private def physicalLocation: Option[DisplayPhysicalLocation] =
      item.locations.collectFirst { case loc: DisplayPhysicalLocation => loc }

    /** Get the physical location type for an item.
      *
      * In practice we know an item will only have at most one physical location.
      */
    def physicalLocationType: Option[DisplayLocationType] =
      physicalLocation.map(_.locationType)

    /** Get the physical access condition for an item.
      *
      * In practice we know an item will only ever have a single access condition.
      */
    def physicalAccessCondition: Option[DisplayAccessCondition] =
      physicalLocation.flatMap(_.accessConditions.headOption)

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
    def isStale: Boolean =
      physicalAccessCondition.forall(_.isStale)
  }

  implicit class DisplayAccessConditionOps(
    accessCondition: DisplayAccessCondition
  ) {
    private def statusId: Option[String] =
      accessCondition.status.map(_.id)

    private def methodId: String =
      accessCondition.method.id

    def isRequestable: Boolean = {
      val isOnlineRequest = methodId.contains("online-request")
      val hasRequestableStatus = statusId.contains("open") || statusId.contains(
        "open-with-advisory"
      ) || statusId.contains("restricted") || statusId.isEmpty

      isOnlineRequest && hasRequestableStatus
    }

    def isStale: Boolean = {
      val isTemporarilyUnavailable =
        statusId.contains("temporarily-unavailable")

      isTemporarilyUnavailable || isRequestable
    }
  }
}
