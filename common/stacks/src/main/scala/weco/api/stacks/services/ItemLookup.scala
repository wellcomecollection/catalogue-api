package weco.api.stacks.services

import weco.api.search.elasticsearch.ElasticsearchError
import weco.catalogue.internal_model.identifiers.{
  CanonicalId,
  IdState,
  SourceIdentifier
}
import weco.catalogue.internal_model.work.Item

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
  ): Future[Seq[Either[ElasticsearchError, Item[IdState.Identified]]]]
}
