package weco.api.stacks.models

import weco.catalogue.internal_model.identifiers.{CanonicalId, SourceIdentifier}

case class StacksItem(
                       id: CanonicalId,
                       sourceIdentifier: SourceIdentifier,
                       status: StacksItemStatus
                     )
