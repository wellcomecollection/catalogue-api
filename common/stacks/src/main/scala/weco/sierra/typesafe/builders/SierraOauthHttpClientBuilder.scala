package weco.sierra.typesafe.builders

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.http.scaladsl.model.headers.BasicHttpCredentials
import com.typesafe.config.Config
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import weco.api.search.models.ApiEnvironment
import weco.http.client.{HttpGet, HttpPost, PekkoHttpClient}
import weco.sierra.http.SierraOauthHttpClient
import weco.typesafe.config.builders.EnrichConfig._

import scala.concurrent.ExecutionContext

// This replaces the implementation of SierraOauthHttpClientBuilder
// from scala-libs as we would like to use the SecretsManagerClient
// to get our config rather than the config file. We should probably
// remove this from scala-libs, after removing any other usages (
// currently the requests service).
object SierraOauthHttpClientBuilder {
  def build(config: Config)(
    implicit
    as: ActorSystem,
    ec: ExecutionContext
  ): SierraOauthHttpClient = {
    build(config, ApiEnvironment.Prod)
  }

  def build(config: Config, environment: ApiEnvironment = ApiEnvironment.Prod)(
    implicit
    as: ActorSystem,
    ec: ExecutionContext
  ): SierraOauthHttpClient = {

    val secretsManagerClientBuilder = SecretsManagerClient.builder()

    val secretsClientForEnv = environment match {
      case ApiEnvironment.Dev =>
        secretsManagerClientBuilder
          .credentialsProvider(
            ProfileCredentialsProvider.create("catalogue-developer")
          )
          .build()
      case _ =>
        secretsManagerClientBuilder.build()
    }

    implicit val secretsClient: SecretsManagerClient = secretsClientForEnv

    // Note: At present, we only use the secrets manager to get the items
    // service key and secret, the requests service uses a different key
    // and secret, which is in the identity account and stored in JSON
    // format, in a single secret. 
    //
    // We should probably refactor this so they both use the same approach!

    val username = config.getStringOption("sierra.api.key") match {
      case Some(key) => key
      case None => getSecretString(
        s"stacks/prod/sierra_api_key"
      )
    }

    val password = config.getStringOption("sierra.api.secret") match {
      case Some(secret) => secret
      case None => getSecretString(
        s"stacks/prod/sierra_api_secret"
      )
    }

    val client = new PekkoHttpClient() with HttpGet with HttpPost {
      override val baseUri: Uri = Uri(
        config.requireString("sierra.api.baseUrl")
      )
    }

    new SierraOauthHttpClient(
      client,
      credentials = BasicHttpCredentials(
        username = username,
        password = password
      )
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