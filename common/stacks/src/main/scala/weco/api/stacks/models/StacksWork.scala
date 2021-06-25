package weco.api.stacks.models

import weco.catalogue.internal_model.identifiers.CanonicalId

case class StacksWork(canonicalId: CanonicalId, items: List[StacksItem])
