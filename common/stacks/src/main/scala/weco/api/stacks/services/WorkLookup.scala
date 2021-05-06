package weco.api.stacks.services

import com.sksamuel.elastic4s.Index
import uk.ac.wellcome.models.Implicits._
import weco.api.search.elasticsearch.{ElasticsearchError, ElasticsearchService}
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed

import scala.concurrent.Future

class WorkLookup(elasticsearchService: ElasticsearchService) {

  /** Returns the Work that corresponds to this canonical ID.
    *
    */
  def byCanonicalId(id: CanonicalId)(
    index: Index): Future[Either[ElasticsearchError, Work[Indexed]]] =
    elasticsearchService.findById[Work[Indexed]](id)(index)
}
