package uk.ac.wellcome.platform.api.common.models.display

import uk.ac.wellcome.api.display.models.{DisplayItem, DisplayItemStatus}
import uk.ac.wellcome.platform.api.common.models.StacksWork

object DisplayStacksWork {
  def apply(stacksWork: StacksWork): DisplayStacksWork =
    DisplayStacksWork(
      id = stacksWork.canonicalId.toString(),
      items = stacksWork.items.map { stacksItem =>
        DisplayItem(
          id = Some(stacksItem.id.value),
          status = Some(
            DisplayItemStatus(
              id = stacksItem.status.id,
              label = stacksItem.status.label
            )
          )
        )
      }
    )
}

case class DisplayStacksWork(
  id: String,
  items: List[DisplayItem],
  `type`: String = "Work"
)
