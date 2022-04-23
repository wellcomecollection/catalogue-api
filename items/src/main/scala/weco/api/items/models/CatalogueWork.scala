package weco.api.items.models

import weco.catalogue.display_model.models.{DisplayIdentifier, DisplayItem}
import weco.catalogue.internal_model.work.{Work, WorkState}

// This is a fork of the DisplayWork model in the display library, which represents
// a work as we receive it from the catalogue API.
case class CatalogueWork(
  id: String,
  title: Option[String],
  identifiers: List[DisplayIdentifier],
  items: List[DisplayItem],
)

object CatalogueWork {
  def apply(work: Work.Visible[WorkState.Indexed]): CatalogueWork =
    CatalogueWork(
      id = work.state.canonicalId.underlying,
      title = work.data.title,
      identifiers = work.identifiers.map { DisplayIdentifier(_) },
      items = work.data.items.map { DisplayItem(_) },
    )
}
