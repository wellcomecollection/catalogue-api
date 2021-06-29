package weco.api.snapshot_generator.config.builders

import com.sksamuel.elastic4s.ElasticClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import weco.catalogue.display_model.ElasticConfig
import weco.elasticsearch.ElasticClientBuilder

object SnapshotGeneratorElasticClientBuilder {

  // We create a new pipeline cluster for every pipeline, and we want the
  // snapshot generator to read from that cluster.
  //
  // We don't want to require a Terraform plan/apply to pick up the change --
  // ElasticConfig is the single source of truth for the API index -- so instead
  // we let the snapshot generator decide which set of secrets to read, which
  // in turn sets which cluster it reads from.

  def apply(): ElasticClient = {
    implicit val secretsClient: SecretsManagerClient =
      SecretsManagerClient.builder().build()

    val pipelineDate = ElasticConfig.indexDate

    ElasticClientBuilder.create(
      hostname = getSecretString(s"elasticsearch/pipeline_storage_$pipelineDate/private_host"),
      port = getSecretString(s"elasticsearch/pipeline_storage_$pipelineDate/port").toInt,
      protocol = getSecretString(s"elasticsearch/pipeline_storage_$pipelineDate/protocol"),
      username = getSecretString(s"elasticsearch/pipeline_storage_$pipelineDate/snapshot_generator/es_username"),
      password = getSecretString(s"elasticsearch/pipeline_storage_$pipelineDate/snapshot_generator/es_password")
    )
  }

  private def getSecretString(id: String)(implicit secretsClient: SecretsManagerClient) = {
    val request =
      GetSecretValueRequest.builder()
        .secretId(id)
        .build()

    secretsClient
      .getSecretValue(request)
      .secretString()
  }
}
