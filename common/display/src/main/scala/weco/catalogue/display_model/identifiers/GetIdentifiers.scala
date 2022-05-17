package weco.catalogue.display_model.identifiers

import weco.catalogue.internal_model.identifiers.IdState

trait GetIdentifiers {
  protected def getIdentifiers(id: IdState): List[DisplayIdentifier] =
    id.allSourceIdentifiers.map(DisplayIdentifier(_))
}
