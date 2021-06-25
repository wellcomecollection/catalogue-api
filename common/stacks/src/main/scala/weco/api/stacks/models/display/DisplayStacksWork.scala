package weco.api.stacks.models.display

import weco.api.stacks.models.StacksWork
import weco.catalogue.display_model.models.DisplayItemStatus
import weco.catalogue.display_model.models.{DisplayItem, DisplayItemStatus}

object DisplayStacksWork {
  def apply(stacksWork: StacksWork): DisplayStacksWork =
    DisplayStacksWork(
      id = stacksWork.canonicalId.toString(),
      items = stacksWork.items.map { stacksItem =>
        DisplayItem(
          id = Some(stacksItem.id.underlying),
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
