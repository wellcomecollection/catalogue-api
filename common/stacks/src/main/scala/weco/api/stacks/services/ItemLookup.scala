package weco.api.stacks.services

import weco.api.search.elasticsearch.ElasticsearchError
import weco.catalogue.internal_model.identifiers.{
  CanonicalId,
  IdState,
  SourceIdentifier
}
import weco.catalogue.internal_model.work.{Item, Work}
import weco.catalogue.internal_model.work.WorkState.Indexed

import scala.concurrent.Future

trait ItemLookup {
  def byCanonicalId(
    itemId: CanonicalId
  ): Future[Either[ElasticsearchError, Item[IdState.Identified]]]

  def bySourceIdentifier(
    sourceIdentifier: SourceIdentifier
  ): Future[Either[ElasticsearchError, Item[IdState.Identified]]]

  def bySourceIdentifiers(
    sourceIdentifiers: Seq[SourceIdentifier]
  ): Future[Seq[ItemLookupResponse]]
}

sealed trait ItemLookupResponse
case class ItemLookupSuccess(
  item: Item[IdState.Identified],
  works: List[Work[Indexed]]
) extends ItemLookupResponse
case class ItemLookupFailure(
  error: ElasticsearchError
) extends ItemLookupResponse
