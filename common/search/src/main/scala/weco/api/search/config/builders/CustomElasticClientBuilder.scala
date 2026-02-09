package weco.api.search.config.builders

import com.sksamuel.elastic4s.ElasticClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import weco.api.search.models.{ApiEnvironment, ClusterConfig}
import weco.elasticsearch.ElasticClientBuilder
import grizzled.slf4j.Logging
import weco.api.search.config.builders.PipelineElasticClientBuilder.{
  getSecretString,
  getSecretsClient
}

object CustomElasticClientBuilder extends Logging {
  def apply(
    clusterConfig: ClusterConfig,
    environment: ApiEnvironment = ApiEnvironment.Prod
  ): ElasticClient = {
    implicit val secretsClient: SecretsManagerClient = getSecretsClient(
      environment)
    val apiKey = getSecretString(clusterConfig.customApiKeySecretPath)
    val hostname = clusterConfig.customHost
    val port = clusterConfig.customPort.getOrElse(9243)
    val protocol = clusterConfig.customProtocol.getOrElse("https")

    info(
      s"Building custom Elasticsearch client for cluster '${clusterConfig.name}' at $protocol://$hostname:$port")

    ElasticClientBuilder.create(
      hostname = hostname,
      port = port,
      protocol = protocol,
      encodedApiKey = apiKey
    )
  }
}
