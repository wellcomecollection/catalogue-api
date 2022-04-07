package weco.api.requests.models

import weco.catalogue.display_model.models.DisplayItem
import weco.catalogue.internal_model.identifiers.CanonicalId

case class RequestedItemWithWork(
  workId: CanonicalId,
  workTitle: Option[String],
  item: DisplayItem
)
