package weco.api.snapshot_generator.models

import com.sksamuel.elastic4s.ElasticClient

trait PipelineElasticClient {
  def forDate(pipelineDate: String): ElasticClient
}
