package weco.api.requests.models.display

import java.time.LocalDate
import weco.api.requests.models.{HoldNote, RequestedItemWithWork}
import weco.catalogue.display_model.models.DisplayItem
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.sierra.models.fields.SierraHold

case class DisplayResultsList(
  results: List[DisplayRequest],
  totalResults: Int,
  `type`: String = "ResultList"
)

object DisplayResultsList {
  def apply(
    itemHolds: List[(SierraHold, RequestedItemWithWork)]
  ): DisplayResultsList =
    DisplayResultsList(
      results = itemHolds.map {
        case (hold, itemWithWork) => DisplayRequest(hold, itemWithWork)
      },
      totalResults = itemHolds.size
    )
}

case class DisplayRequest(
  workTitle: Option[String],
  workId: CanonicalId,
  item: DisplayItem,
  pickupDate: Option[LocalDate],
  pickupLocation: DisplayLocationDescription,
  status: DisplayRequestStatus,
  `type`: String = "Request"
)

object DisplayRequest {
  def apply(
    hold: SierraHold,
    itemWithWork: RequestedItemWithWork
  ): DisplayRequest =
    DisplayRequest(
      workTitle = itemWithWork.workTitle,
      workId = itemWithWork.workId,
      item = DisplayItem(
        item = itemWithWork.item,
        includesIdentifiers = true
      ),
      pickupDate = hold.note.flatMap(HoldNote.parsePickupDate),
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
