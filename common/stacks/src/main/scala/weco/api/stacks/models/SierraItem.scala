package weco.api.stacks.models

import weco.catalogue.source_model.sierra.identifiers.SierraItemNumber

case class SierraItemStatus(
  code: String,
  display: String
)

case class SierraItem(
  id: SierraItemNumber,
  deleted: Boolean,
  suppressed: Boolean = false,
  holdCount: Int = 0,
  status: Option[SierraItemStatus]
)
