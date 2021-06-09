package weco.api.stacks.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, OAuth2BearerToken}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.api.http.TokenExchange

import java.time.Instant
import scala.concurrent.Future

trait SierraAuthenticatedHttpClient
  extends HttpClient
    with TokenExchange[BasicHttpCredentials, OAuth2BearerToken] {

  val tokenUri: Uri
  val credentials: BasicHttpCredentials

  implicit val system: ActorSystem
  implicit val unmarshal: Unmarshaller[HttpResponse, AccessToken]

  protected def makeRequest(request: HttpRequest, token: OAuth2BearerToken): Future[HttpResponse]

  // This implements the Client Credentials flow, as described in the Sierra docs:
  // https://techdocs.iii.com/sierraapi/Content/zReference/authClient.htm
  //
  // We make a request with a client key and secret, retrieve an access token which
  // lasts an hour, and use that for future requests.  When the hour is up, we have
  // to fetch a new token.
  //
  private case class AccessToken(
    @JsonKey("access_token") accessToken: String,
    @JsonKey("expires_in") expiresIn: Int
  )

  override protected def getNewToken(
                                      credentials: BasicHttpCredentials
                                    ): Future[(OAuth2BearerToken, Instant)] = {
    val postRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = tokenUri,
      headers = List(Authorization(credentials))
    )

    for {
      tokenResponse <- Http().singleRequest(postRequest)

      accessToken <- Unmarshal(tokenResponse).to[AccessToken]

      result = (
        OAuth2BearerToken(accessToken.accessToken),
        Instant.now().plusSeconds(accessToken.expiresIn)
      )
    } yield result
  }

  override protected def makeRequest(request: HttpRequest): Future[HttpResponse] =
    for {
      token <- getToken(credentials)

      response <- makeRequest(request, token)
    } yield response
}
