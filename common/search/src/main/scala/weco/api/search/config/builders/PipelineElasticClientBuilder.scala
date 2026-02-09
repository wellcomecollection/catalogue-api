package weco.api.search.config.builders

import com.sksamuel.elastic4s.ElasticClient
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import weco.api.search.models.{ApiEnvironment, ElasticConfig}
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
    pipelineDate: String = ElasticConfig.pipelineDate,
    environment: ApiEnvironment = ApiEnvironment.Prod
  ): ElasticClient = {
    implicit val secretsClientForEnv: SecretsManagerClient = getSecretsClient(
      environment)
    val hostType = environment match {
      case ApiEnvironment.Dev => "public_host"
      case _                  => "private_host"
    }

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

  def getSecretsClient(environment: ApiEnvironment): SecretsManagerClient = {
    val secretsManagerClientBuilder = SecretsManagerClient.builder()
    environment match {
      case ApiEnvironment.Dev =>
        secretsManagerClientBuilder
          .credentialsProvider(
            ProfileCredentialsProvider.create("catalogue-developer")
          )
          .build()
      case _ =>
        secretsManagerClientBuilder.build()
    }
  }

  def getSecretString(
    id: String
  )(implicit secretsClient: SecretsManagerClient): String = {
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
