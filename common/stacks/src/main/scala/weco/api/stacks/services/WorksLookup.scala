package weco.api.stacks.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticError, Index}
import uk.ac.wellcome.models.Implicits._
import weco.api.search.elasticsearch.ElasticsearchService
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed

import scala.concurrent.{ExecutionContext, Future}

class WorksLookup(elasticsearchService: ElasticsearchService)(
  implicit ec: ExecutionContext
) {
  def byWorkId(id: CanonicalId)(index: Index): Future[Either[ElasticError, Option[Work[Indexed]]]] =
    elasticsearchService.findById[Work[Indexed]](id)(index)

  def byItemId(id: CanonicalId)(index: Index): Future[Either[ElasticError, Option[Work[Indexed]]]] = {
    val searchRequest =
      search(index)
        .query(
          boolQuery.filter(
            termQuery("data.items.id.canonicalId", id.underlying),
            termQuery(field = "type", value = "Visible")
          )
        )
        .size(1)

    elasticsearchService.findBySearch[Work[Indexed]](searchRequest).map {
      case Left(err)    => Left(err)
      case Right(works) => Right(works.headOption)
    }
  }
}
