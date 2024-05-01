package weco.api.search.config.builders

import com.sksamuel.elastic4s.ElasticClient
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import weco.api.search.models.{ApiEnvironment, PipelineClusterElasticConfig}
import weco.elasticsearch.ElasticClientBuilder

object PipelineElasticClientBuilder {

  // We create a new pipeline cluster for every pipeline, and we want the
  // services to read from that cluster.
  //
  // We don't want to require a Terraform plan/apply to pick up the change --
  // ElasticConfig is the single source of truth for the API index -- so instead
  // we let the services decide which set of secrets to read, which in turn sets
  // which cluster they read from.

  def apply(
    serviceName: String,
    pipelineDate: String = PipelineClusterElasticConfig.pipelineDate,
    environment: ApiEnvironment = ApiEnvironment.Prod
  ): ElasticClient = {

    val secretsManagerClientBuilder = SecretsManagerClient.builder()

    val (hostType, secretsClientForEnv) = environment match {
      case ApiEnvironment.Dev =>
        (
          "public_host",
          secretsManagerClientBuilder
            .credentialsProvider(
              ProfileCredentialsProvider.create("catalogue-developer"))
            .build())
      case _ =>
        ("private_host", secretsManagerClientBuilder.build())
    }

    implicit val secretsClient: SecretsManagerClient = secretsClientForEnv

    val hostname = getSecretString(
      s"elasticsearch/pipeline_storage_$pipelineDate/$hostType"
    )

    val port = getSecretString(
      s"elasticsearch/pipeline_storage_$pipelineDate/port"
    ).toInt
    val protocol = getSecretString(
      s"elasticsearch/pipeline_storage_$pipelineDate/protocol"
    )
    val apiKey = getSecretString(
      s"elasticsearch/pipeline_storage_$pipelineDate/$serviceName/api_key"
    )

    ElasticClientBuilder.create(
      hostname = hostname,
      port = port,
      protocol = protocol,
      encodedApiKey = apiKey
    )
  }

  private def getSecretString(
    id: String
  )(implicit secretsClient: SecretsManagerClient) = {
    val request =
      GetSecretValueRequest
        .builder()
        .secretId(id)
        .build()

    secretsClient
      .getSecretValue(request)
      .secretString()
  }
}
