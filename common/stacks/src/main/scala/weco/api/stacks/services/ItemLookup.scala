package weco.api.stacks.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, Index}
import uk.ac.wellcome.models.Implicits._
import weco.api.search.elasticsearch.{
  DocumentNotFoundError,
  ElasticsearchError,
  ElasticsearchService
}
import weco.catalogue.internal_model.identifiers.{
  CanonicalId,
  IdState,
  SourceIdentifier
}
import weco.catalogue.internal_model.work.{Item, Work}
import weco.catalogue.internal_model.work.WorkState.Indexed

import scala.concurrent.{ExecutionContext, Future}

class ItemLookup(elasticsearchService: ElasticsearchService)(
  implicit ec: ExecutionContext
) {

  /** Returns the SourceIdentifier of the item that corresponds to this
    * canonical ID.
    *
    */
  def byCanonicalId(itemId: CanonicalId)(
    index: Index): Future[Either[ElasticsearchError, SourceIdentifier]] = {
    val searchRequest =
      search(index)
        .query(
          boolQuery.filter(
            termQuery("data.items.id.canonicalId", itemId.underlying),
            termQuery(field = "type", value = "Visible")
          )
        )
        .size(1)

    elasticsearchService.findBySearch[Work[Indexed]](searchRequest).map {
      case Left(err) => Left(err)
      case Right(works) =>
        val item =
          works
            .flatMap { _.data.items }
            .collectFirst {
              case Item(IdState.Identified(id, sourceIdentifier, _), _, _)
                  if id == itemId =>
                sourceIdentifier
            }

        item match {
          case Some(it) => Right(it)
          case None     => Left(DocumentNotFoundError(itemId))
        }
    }
  }

  /** Returns the canonical ID of the item with this source identifier.
    *
    */
  def bySourceIdentifier(sourceIdentifier: SourceIdentifier)(
    index: Index): Future[Either[ElasticsearchError, CanonicalId]] = {
    // TODO: What if we get something with the right value but wrong type?
    // We should be able to filter by ontologyType and IdentifierType.
    val searchRequest =
      search(index)
        .query(
          boolQuery
            .must(termQuery(field = "type", value = "Visible"))
            .should(
              termQuery(
                "data.items.id.sourceIdentifier.value",
                sourceIdentifier.value),
            )
        )
        .size(10)

    elasticsearchService.findBySearch[Work[Indexed]](searchRequest).map {
      case Left(err) => Left(err)
      case Right(works) =>
        val item =
          works
            .flatMap { _.data.items }
            .collectFirst {
              case Item(id @ IdState.Identified(_, _, _), _, _)
                  if id.sourceIdentifier == sourceIdentifier =>
                id.canonicalId
            }

        item match {
          case Some(it) => Right(it)
          case None     => Left(DocumentNotFoundError(sourceIdentifier))
        }
    }
  }
}

object ItemLookup {
  def apply(elasticClient: ElasticClient)(
    implicit ec: ExecutionContext): ItemLookup =
    new ItemLookup(
      elasticsearchService = new ElasticsearchService(elasticClient)
    )
}
