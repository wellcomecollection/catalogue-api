package weco.api.stacks.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, OAuth2BearerToken}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import uk.ac.wellcome.platform.api.http.TokenExchange
import weco.api.stacks.models.SierraAccessToken

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

trait SierraAuthenticatedHttpClient
  extends HttpClient
    with TokenExchange[BasicHttpCredentials, OAuth2BearerToken] {

  val tokenPath: Path
  val credentials: BasicHttpCredentials

  implicit val system: ActorSystem
  implicit val um: Unmarshaller[HttpResponse, SierraAccessToken]
  implicit val ec: ExecutionContext

  protected def makeSingleRequest(request: HttpRequest): Future[HttpResponse]

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

  // This implements the Client Credentials flow, as described in the Sierra docs:
  // https://techdocs.iii.com/sierraapi/Content/zReference/authClient.htm
  //
  // We make a request with a client key and secret, retrieve an access token which
  // lasts an hour, and use that for future requests.  When the hour is up, we have
  // to fetch a new token.
  //
  override protected def getNewToken(
    credentials: BasicHttpCredentials
  ): Future[(OAuth2BearerToken, Instant)] =
    for {
      tokenResponse <- post[Unit](
        path = tokenPath,
        headers = List(Authorization(credentials))
      )

      accessToken <- Unmarshal(tokenResponse).to[SierraAccessToken]

      result = (
        OAuth2BearerToken(accessToken.access_token),
        Instant.now().plusSeconds(accessToken.expires_in)
      )
    } yield result

  private def fetchTokenAndMakeRequest(request: HttpRequest): Future[HttpResponse] =
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

  override protected def makeRequest(request: HttpRequest): Future[HttpResponse] =
    // The method for fetching a token will call this method, so if
    // there's already authentication skip fetching it again.
    request.headers.collectFirst { case auth: Authorization => auth } match {
      case Some(_) => makeSingleRequest(request)
      case None    => fetchTokenAndMakeRequest(request)
    }
}
