package weco.api.stacks.services.memory

import weco.api.search.elasticsearch.{DocumentNotFoundError, ElasticsearchError}
import weco.api.stacks.services.ItemLookup
import weco.catalogue.internal_model.identifiers.{
  CanonicalId,
  IdState,
  SourceIdentifier
}
import weco.catalogue.internal_model.work.Item

import scala.concurrent.Future

class MemoryItemLookup(items: Seq[Item[IdState.Identified]])
    extends ItemLookup {
  override def byCanonicalId(
    itemId: CanonicalId
  ): Future[Either[ElasticsearchError, Item[IdState.Identified]]] =
    Future.successful(
      items.find(_.id.canonicalId == itemId) match {
        case Some(it) => Right(it)
        case None     => Left(DocumentNotFoundError(itemId))
      }
    )

  override def bySourceIdentifier(
    sourceIdentifiers: Seq[SourceIdentifier]
  ): Future[Seq[Either[ElasticsearchError, Item[IdState.Identified]]]] =
    Future.successful {
      val foldInitial =
        Seq.empty[Either[ElasticsearchError, Item[IdState.Identified]]]

      sourceIdentifiers.foldLeft(foldInitial) { (acc, sourceIdentifier) =>
        items.find(_.id.sourceIdentifier == sourceIdentifier) match {
          case Some(item) => acc :+ Right(item)
          case None       => acc :+ Left(DocumentNotFoundError(sourceIdentifier))
        }
      }
    }
}
