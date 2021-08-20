package weco.api.requests.models

import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.identifiers.IdState.Identified
import weco.catalogue.internal_model.work.Item

case class RequestedItemWithWork(workId: CanonicalId, workTitle: Option[String], item: Item[Identified])
