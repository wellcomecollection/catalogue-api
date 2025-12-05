package weco.api.snapshot_generator.models

import weco.api.search.elasticsearch.ResilientElasticClient

trait PipelineElasticClient {
  def forDate(pipelineDate: String): ResilientElasticClient
}
