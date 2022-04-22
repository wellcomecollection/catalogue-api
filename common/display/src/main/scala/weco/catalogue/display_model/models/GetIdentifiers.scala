package weco.catalogue.display_model.models

import weco.catalogue.internal_model.identifiers.IdState

trait GetIdentifiers {
  protected def getIdentifiers(id: IdState): List[DisplayIdentifier] =
    id.allSourceIdentifiers.map(DisplayIdentifier(_))

  protected def getIdentifiers(id: IdState, includesIdentifiers: Boolean): Option[List[DisplayIdentifier]] =
    if (includesIdentifiers)
      Option(getIdentifiers(id)).filter(_.nonEmpty)
    else
      None
}
