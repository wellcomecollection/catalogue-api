package weco.api.stacks.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, OAuth2BearerToken}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import uk.ac.wellcome.platform.api.http.TokenExchange
import weco.api.stacks.models.SierraAccessToken

import java.time.Instant
import scala.concurrent.Future

trait SierraAuthenticatedHttpClient
  extends HttpClient
    with TokenExchange[BasicHttpCredentials, OAuth2BearerToken] {

  val tokenUri: Uri
  val credentials: BasicHttpCredentials

  implicit val system: ActorSystem
  implicit val unmarshaller: Unmarshaller[HttpResponse, SierraAccessToken]

  protected def makeSingleRequest(request: HttpRequest): Future[HttpResponse]

  // This implements the Client Credentials flow, as described in the Sierra docs:
  // https://techdocs.iii.com/sierraapi/Content/zReference/authClient.htm
  //
  // We make a request with a client key and secret, retrieve an access token which
  // lasts an hour, and use that for future requests.  When the hour is up, we have
  // to fetch a new token.
  //
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

      accessToken <- Unmarshal(tokenResponse).to[SierraAccessToken]

      result = (
        OAuth2BearerToken(accessToken.access_token),
        Instant.now().plusSeconds(accessToken.expires_in)
      )
    } yield result
  }

  override def makeRequest(request: HttpRequest): Future[HttpResponse] =
    for {
      token <- getToken(credentials)

      authenticatedRequest = {
        // We're going to set our own Authorization header on this request
        // using the token, so there shouldn't be one already.
        //
        // Are multiple Authorization headers allowed by HTTP?  It doesn't matter,
        // it's not something we should be doing.
        val existingAuthHeaders = request.headers.collect {
          case auth: Authorization => auth
        }
        require(existingAuthHeaders.isEmpty, s"HTTP request already has auth headers: $request")

        request.copy(
          headers = request.headers :+ Authorization(token)
        )
      }

      response <- makeSingleRequest(authenticatedRequest)
    } yield response
}
