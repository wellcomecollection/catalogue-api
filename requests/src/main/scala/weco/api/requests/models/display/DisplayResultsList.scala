package weco.api.requests.models.display

import java.time.Instant

import weco.catalogue.display_model.models.DisplayItem
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.work.Item
import weco.sierra.models.fields.SierraHold

case class DisplayResultsList(
  results: List[DisplayRequest],
  totalResults: Int,
  `type`: String = "ResultList"
)

object DisplayResultsList {
  def apply(
    itemHolds: List[(SierraHold, Item[IdState.Identified])]
  ): DisplayResultsList =
    DisplayResultsList(
      results = itemHolds.map {
        case (hold, item) => DisplayRequest(hold, item)
      },
      totalResults = itemHolds.size
    )
}

case class DisplayRequest(
  item: DisplayItem,
  pickupDate: Option[Instant],
  pickupLocation: DisplayLocationDescription,
  status: DisplayRequestStatus,
  `type`: String = "Request"
)

object DisplayRequest {
  def apply(
    hold: SierraHold,
    item: Item[IdState.Identified]
  ): DisplayRequest =
    DisplayRequest(
      item = DisplayItem(
        item = item,
        includesIdentifiers = true
      ),
      pickupDate = hold.pickupByDate,
      pickupLocation = DisplayLocationDescription(
        id = hold.pickupLocation.code,
        label = hold.pickupLocation.name
      ),
      status = DisplayRequestStatus(
        id = hold.status.code,
        label = hold.status.name
      )
    )
}

case class DisplayLocationDescription(
  id: String,
  label: String,
  `type`: String = "LocationDescription"
)

case class DisplayRequestStatus(
  id: String,
  label: String,
  `type`: String = "RequestStatus"
)
