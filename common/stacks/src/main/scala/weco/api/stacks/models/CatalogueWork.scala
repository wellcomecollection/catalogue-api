package weco.api.stacks.models

import io.circe.Json

case class CatalogueWork(
  id: String,
  title: Option[String],
  identifiers: List[CatalogueIdentifier],
  items: List[Json]
)
