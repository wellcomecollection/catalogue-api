package weco.api.stacks.models.display

import java.time.Instant
import weco.api.stacks.models.{
  StacksHold,
  StacksHoldStatus,
  StacksPickupLocation,
  StacksUserHolds
}
import weco.catalogue.display_model.models.DisplayItem

object DisplayResultsList {
  def apply(
    stacksUserHolds: StacksUserHolds
  ): DisplayResultsList =
    DisplayResultsList(
      results = stacksUserHolds.holds.map(DisplayRequest(_)),
      totalResults = stacksUserHolds.holds.size
    )
}

case class DisplayResultsList(
  results: List[DisplayRequest],
  totalResults: Int,
  `type`: String = "ResultList"
)

object DisplayRequest {
  def apply(hold: StacksHold): DisplayRequest = {
    DisplayRequest(
      // TODO: Remove this .get!
      item = DisplayItem(
        item = hold.item.get,
        includesIdentifiers = true
      ),
      pickupDate = hold.pickup.pickUpBy,
      pickupLocation = DisplayLocationDescription(
        hold.pickup.location
      ),
      status = DisplayRequestStatus(
        hold.status
      )
    )
  }
}

case class DisplayRequest(
  item: DisplayItem,
  pickupDate: Option[Instant],
  pickupLocation: DisplayLocationDescription,
  status: DisplayRequestStatus,
  `type`: String = "Request"
)

object DisplayLocationDescription {
  def apply(location: StacksPickupLocation): DisplayLocationDescription =
    DisplayLocationDescription(
      id = location.id,
      label = location.label
    )
}

case class DisplayLocationDescription(
  id: String,
  label: String,
  `type`: String = "LocationDescription"
)

object DisplayRequestStatus {
  def apply(holdStatus: StacksHoldStatus): DisplayRequestStatus =
    DisplayRequestStatus(
      id = holdStatus.id,
      label = holdStatus.label
    )
}

case class DisplayRequestStatus(
  id: String,
  label: String,
  `type`: String = "RequestStatus"
)
