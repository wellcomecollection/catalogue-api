package weco.api.stacks.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticError, Index}
import uk.ac.wellcome.models.Implicits._
import weco.api.search.elasticsearch.ElasticsearchService
import weco.catalogue.internal_model.identifiers.{CanonicalId, IdState}
import weco.catalogue.internal_model.work.{Item, Work}
import weco.catalogue.internal_model.work.WorkState.Indexed

import scala.concurrent.{ExecutionContext, Future}

class ItemLookup(elasticsearchService: ElasticsearchService)(
  implicit ec: ExecutionContext
) {
  def byId(itemId: CanonicalId)(index: Index): Future[Either[ElasticError, Option[Item[IdState.Identified]]]] = {
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
      case Left(err)    => Left(err)
      case Right(works) =>
        Right(
          works
            .flatMap { _.data.items }
            .collectFirst {
              // The .asInstanceOf here will be a no-op at runtime, and is just so
              // the compiler knows this will always be an Item[IdState.Identified]
              case item@Item(IdState.Identified(id, _, _), _, _) if id == itemId
              => item.asInstanceOf[Item[IdState.Identified]]
            }
        )
    }
  }
}
