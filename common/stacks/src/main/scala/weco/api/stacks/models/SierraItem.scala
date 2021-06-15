package weco.api.stacks.models

import weco.catalogue.source_model.sierra.identifiers.SierraItemNumber

case class SierraItemStatus(
  code: String,
  display: String
)

case class SierraItem(
  id: SierraItemNumber,
  deleted: Boolean,
  status: Option[SierraItemStatus]
)
