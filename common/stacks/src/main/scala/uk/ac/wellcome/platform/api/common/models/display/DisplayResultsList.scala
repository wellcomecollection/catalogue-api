package uk.ac.wellcome.platform.api.common.models.display

import java.time.Instant
import uk.ac.wellcome.api.display.models.DisplayItem
import uk.ac.wellcome.platform.api.common.models._
import weco.catalogue.display_model.models.{DisplayIdentifier, DisplayItem}

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
      item = new DisplayItem(
        id = hold.canonicalId.map { _.underlying },
        identifiers = Some(List(DisplayIdentifier(hold.sourceIdentifier)))
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
