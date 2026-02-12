package weco.api.search.config.builders

import com.sksamuel.elastic4s.ElasticClient
import grizzled.slf4j.Logging
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import weco.api.search.models.{ApiEnvironment, ElasticConfig}
import weco.elasticsearch.ElasticClientBuilder

object PipelineElasticClientBuilder extends Logging {

  // We create a new pipeline cluster for every pipeline, and we want the
  // services to read from that cluster.
  //
  // We don't want to require a Terraform plan/apply to pick up the change --
  // ElasticConfig is the single source of truth for the API index -- so instead
  // we let the services decide which set of secrets to read, which in turn sets
  // which cluster they read from.

  def apply(
    serviceName: String,
    pipelineDate: String = ElasticConfig.defaultPipelineDate,
    environment: ApiEnvironment = ApiEnvironment.Prod,
    clusterConfig: ElasticConfig = ElasticConfig()
  ): ElasticClient = {
    implicit val secretsClientForEnv: SecretsManagerClient = getSecretsClient(
      environment)

    val pipelinePrefix = s"elasticsearch/pipeline_storage_$pipelineDate";

    val hostType = environment match {
      case ApiEnvironment.Dev => "public_host"
      case _                  => "private_host"
    }
    val hostname = getSecretString(
      clusterConfig.hostSecretPath.getOrElse(s"$pipelinePrefix/$hostType"))

    val port = getSecretString(
      clusterConfig.portSecretPath.getOrElse(s"$pipelinePrefix/port")).toInt
    val protocol = getSecretString(
      clusterConfig.protocolSecretPath.getOrElse(s"$pipelinePrefix/protocol"))
    val apiKey = getSecretString(
      clusterConfig.apiKeySecretPath.getOrElse(
        s"$pipelinePrefix/$serviceName/api_key"))

    info(
      s"Building custom Elasticsearch client for cluster '${clusterConfig.name}' at $protocol://$hostname:$port")

    ElasticClientBuilder.create(
      hostname = hostname,
      port = port,
      protocol = protocol,
      encodedApiKey = apiKey
    )
  }

  private def getSecretsClient(environment: ApiEnvironment) = {
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
