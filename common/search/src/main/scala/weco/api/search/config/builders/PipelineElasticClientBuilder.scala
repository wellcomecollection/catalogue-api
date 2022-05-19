package weco.api.search.config.builders

import com.sksamuel.elastic4s.ElasticClient
import grizzled.slf4j.Logging
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import weco.api.search.models.PipelineClusterElasticConfig
import weco.elasticsearch.ElasticClientBuilder

/**
  * Build an ElasticClient that will connect to the appropriate database
  * as specified for this environment.
  *
  * The settings may come either from environment variables
  * (if API_SETTINGS_MODE is environment) or from the AWS Secrets Manager
  */
object PipelineElasticClientBuilder extends Logging {
  def apply(serviceName: String): ElasticClient =
    sys.env.get("API_SETTINGS_MODE") match {
      case None | Some("secretsmanager") =>
        info("Building Elastic client from secrets manager")
        SecretsElasticClientBuilder(serviceName)
      case Some("environment") =>
        info("Building Elastic client from environment variables")
        EnvElasticClientBuilder()
      case Some(wrongMode) =>
        throw new IllegalArgumentException(
          s"Unexpected API_SETTINGS_MODE: $wrongMode, must be one of 'secretsmanager' (default) or 'environment'"
        )
    }
}

/**
  * Build an ElasticClient from Environment Variables
  * Use this to run the API without accessing the AWS Secrets Manager.
  * For example, to run against a non-pipeline database such as a local copy.
  */
object EnvElasticClientBuilder {
  def apply(): ElasticClient = {
    val hostname = sys.env("API_ELASTIC_HOST")
    val port = sys.env("API_ELASTIC_PORT").toInt
    val protocol = sys.env("API_ELASTIC_PROTOCOL")
    val username = sys.env("API_ELASTIC_USERNAME")
    val password = sys.env("API_ELASTIC_PASSWORD")

    ElasticClientBuilder.create(
      hostname = hostname,
      port = port,
      protocol = protocol,
      username = username,
      password = password
    )
  }
}

object SecretsElasticClientBuilder {

  // We create a new pipeline cluster for every pipeline, and we want the
  // services to read from that cluster.
  //
  // We don't want to require a Terraform plan/apply to pick up the change --
  // ElasticConfig is the single source of truth for the API index -- so instead
  // we let the services decide which set of secrets to read, which in turn sets
  // which cluster they read from.

  def apply(serviceName: String): ElasticClient = {
    implicit val secretsClient: SecretsManagerClient =
      SecretsManagerClient.builder().build()

    val pipelineDate = PipelineClusterElasticConfig.pipelineDate

    val hostname = getSecretString(
      s"elasticsearch/pipeline_storage_$pipelineDate/private_host"
    )
    val port = getSecretString(
      s"elasticsearch/pipeline_storage_$pipelineDate/port"
    ).toInt
    val protocol = getSecretString(
      s"elasticsearch/pipeline_storage_$pipelineDate/protocol"
    )
    val username = getSecretString(
      s"elasticsearch/pipeline_storage_$pipelineDate/$serviceName/es_username"
    )
    val password = getSecretString(
      s"elasticsearch/pipeline_storage_$pipelineDate/$serviceName/es_password"
    )

    ElasticClientBuilder.create(
      hostname = hostname,
      port = port,
      protocol = protocol,
      username = username,
      password = password
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
