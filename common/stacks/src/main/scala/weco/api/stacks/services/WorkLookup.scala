package weco.api.stacks.services

import com.sksamuel.elastic4s.{ElasticError, Index}
import uk.ac.wellcome.models.Implicits._
import weco.api.search.elasticsearch.ElasticsearchService
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed

import scala.concurrent.Future

class WorkLookup(elasticsearchService: ElasticsearchService) {
  def byId(id: CanonicalId)(index: Index): Future[Either[ElasticError, Option[Work[Indexed]]]] =
    elasticsearchService.findById[Work[Indexed]](id)(index)
}
