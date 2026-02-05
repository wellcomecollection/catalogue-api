package weco.api.search.config.builders

import com.sksamuel.elastic4s.ElasticClient
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import weco.api.search.models.{ApiEnvironment, ClusterConfig}
import weco.elasticsearch.ElasticClientBuilder
import grizzled.slf4j.Logging

object MultiClusterElasticClientBuilder extends Logging {

  /** Build an ElasticClient for a specific cluster configuration */
  def buildClient(
    clusterConfig: ClusterConfig,
    serviceName: String,
    environment: ApiEnvironment = ApiEnvironment.Prod
  ): ElasticClient =
    // If custom configuration is provided, use it directly (for serverless, etc.)
    if (clusterConfig.customHost.isDefined) {
      buildCustomClient(clusterConfig, serviceName, environment)
    } else if (clusterConfig.pipelineDate.isDefined) {
      // Otherwise, use the standard pipeline cluster builder
      PipelineElasticClientBuilder(
        serviceName = serviceName,
        pipelineDate = clusterConfig.pipelineDate.get,
        environment = environment
      )
    } else {
      throw new IllegalArgumentException(
        s"ClusterConfig '${clusterConfig.name}' must have either pipelineDate or custom connection details"
      )
    }

  private def buildCustomClient(
    clusterConfig: ClusterConfig,
    serviceName: String,
    environment: ApiEnvironment
  ): ElasticClient = {
    val secretsManagerClientBuilder = SecretsManagerClient
      .builder()
      .region(Region.EU_WEST_1)

    val secretsClient = environment match {
      case ApiEnvironment.Dev =>
        secretsManagerClientBuilder
          .credentialsProvider(
            ProfileCredentialsProvider.create("catalogue-developer")
          )
          .build()
      case _ =>
        secretsManagerClientBuilder.build()
    }

    val hostname = clusterConfig.customHost.getOrElse(
      throw new IllegalArgumentException(
        "customHost is required for custom cluster")
    )

    val port = clusterConfig.customPort.getOrElse(9243)
    val protocol = clusterConfig.customProtocol.getOrElse("https")

    // Get API key from secrets manager
    val apiKey = clusterConfig.customApiKeySecretPath match {
      case Some(secretPath) =>
        val request = GetSecretValueRequest
          .builder()
          .secretId(secretPath)
          .build()
        secretsClient.getSecretValue(request).secretString()
      case None =>
        throw new IllegalArgumentException(
          s"customApiKeySecretPath is required for custom cluster '${clusterConfig.name}'"
        )
    }

    secretsClient.close()

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
