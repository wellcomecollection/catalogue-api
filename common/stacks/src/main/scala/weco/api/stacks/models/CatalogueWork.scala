package weco.api.stacks.models

import weco.catalogue.display_model.identifiers.DisplayIdentifier
import weco.catalogue.display_model.work.DisplayItem

// This represents a work as we receive it from the catalogue API.
case class CatalogueWork(
  id: String,
  title: Option[String],
  identifiers: List[DisplayIdentifier],
  items: List[DisplayItem]
)
