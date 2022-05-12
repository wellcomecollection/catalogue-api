package weco.api.stacks.models

import weco.catalogue.display_model.work.DisplayItem
import weco.catalogue.internal_model.work.{Work, WorkState}

case class CatalogueWork(
  id: String,
  title: Option[String],
  identifiers: List[CatalogueIdentifier],
  items: List[DisplayItem]
)

object CatalogueWork {
  def apply(work: Work.Visible[WorkState.Indexed]): CatalogueWork =
    CatalogueWork(
      id = work.state.canonicalId.underlying,
      title = work.data.title,
      identifiers = work.identifiers.map { id =>
        CatalogueIdentifier(
          identifierType = CatalogueIdentifierType(id = id.identifierType.id),
          value = id.value
        )
      },
      items = work.data.items.map { DisplayItem(_) }
    )
}
