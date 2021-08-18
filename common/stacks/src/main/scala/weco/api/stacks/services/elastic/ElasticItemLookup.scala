package weco.api.stacks.services.elastic

import com.sksamuel.elastic4s.ElasticDsl.{boolQuery, search, termQuery}
import com.sksamuel.elastic4s.requests.searches.MultiSearchRequest
import com.sksamuel.elastic4s.{ElasticClient, Index}
import weco.api.search.elasticsearch.{DocumentNotFoundError, ElasticsearchError, ElasticsearchService}
import weco.api.stacks.services.{ItemLookup, ItemLookupFailure, ItemLookupResponse, ItemLookupSuccess}
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.identifiers.{CanonicalId, IdState, SourceIdentifier}
import weco.catalogue.internal_model.work.{Item, Work}
import weco.catalogue.internal_model.work.WorkState.Indexed

import scala.concurrent.{ExecutionContext, Future}

class ElasticItemLookup(
  elasticsearchService: ElasticsearchService,
  index: Index
)(
  implicit ec: ExecutionContext
) extends ItemLookup {

  /** Returns the SourceIdentifier of the item that corresponds to this
    * canonical ID.
    *
    */
  def byCanonicalId(
    itemId: CanonicalId
  ): Future[Either[ElasticsearchError, Item[IdState.Identified]]] = {
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
              case item @ Item(IdState.Identified(id, _, _), _, _, _)
                  if id == itemId =>
                // This .asInstanceOf[] is a no-op to help the compiler see what
                // we can see by reading the code.
                item.asInstanceOf[Item[IdState.Identified]]
            }

        item match {
          case Some(it) => Right(it)
          case None     => Left(DocumentNotFoundError(itemId))
        }
    }
  }

  private def buildSourceIdentifierSearchRequest(
    index: Index
  )(sourceIdentifier: SourceIdentifier) =
    search(index)
      .query(
        boolQuery
          .must(termQuery(field = "type", value = "Visible"))
          .should(
            termQuery(
              "data.items.id.sourceIdentifier.value",
              sourceIdentifier.value
            )
          )
      )

  val searchRequestOnIndex = buildSourceIdentifierSearchRequest(index)(_)

  def matchItemsOnWorksBySourceIdentifier(
    works: Seq[Work[Indexed]],
    sourceIdentifier: SourceIdentifier
  ) =
    works
      .flatMap { _.data.items }
      .collect {
        case item @ Item(id @ IdState.Identified(_, _, _), _, _, _)
            if id.sourceIdentifier == sourceIdentifier =>
          // This .asInstanceOf[] is a no-op to help the compiler see what
          // we can see by reading the code.
          item.asInstanceOf[Item[IdState.Identified]]
      }

  def bySourceIdentifier(
    sourceIdentifier: SourceIdentifier
  ): Future[Either[ElasticsearchError, Item[IdState.Identified]]] = {
    val searchRequest = searchRequestOnIndex(sourceIdentifier)

    elasticsearchService.findBySearch[Work[Indexed]](searchRequest).map {
      case Left(err) => Left(err)
      case Right(works) =>
        matchItemsOnWorksBySourceIdentifier(works, sourceIdentifier).toList match {
          case item :: _ => Right(item)
          case List()    => Left(DocumentNotFoundError(sourceIdentifier))
        }
    }
  }

  override def bySourceIdentifiers(
    sourceIdentifiers: Seq[SourceIdentifier]
  ): Future[Seq[ItemLookupResponse]] = {
    val multiSearchRequest = MultiSearchRequest(
      sourceIdentifiers.map(searchRequestOnIndex)
    )

    elasticsearchService
      .findByMultiSearch[Work[Indexed]](multiSearchRequest)
      .map {
        _.zip(sourceIdentifiers).map {
          case (Right(works), srcId) =>
            matchItemsOnWorksBySourceIdentifier(works, srcId).toList match {
              case List(item) => ItemLookupSuccess(item, works.toList)
              case item :: _  => ItemLookupSuccess(item, works.toList)
              case List()     => ItemLookupFailure(DocumentNotFoundError(srcId))
            }

          case (Left(err), _) => ItemLookupFailure(err)
        }
      }
  }
}

object ElasticItemLookup {
  def apply(elasticClient: ElasticClient, index: Index)(
    implicit ec: ExecutionContext
  ): ElasticItemLookup =
    new ElasticItemLookup(
      elasticsearchService = new ElasticsearchService(elasticClient),
      index = index
    )
}
