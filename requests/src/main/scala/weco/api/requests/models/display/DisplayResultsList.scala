package weco.api.requests.models.display

import io.circe.Json

import java.time.LocalDate
import weco.api.requests.models.{HoldNote, RequestedItemWithWork}
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
        case (hold, itemWithWork) =>
          DisplayRequest(hold, itemWithWork)
      },
      totalResults = itemHolds.size
    )
}

case class DisplayRequest(
  workTitle: Option[String],
  workId: String,
  item: Json,
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
      item = itemWithWork.item,
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
