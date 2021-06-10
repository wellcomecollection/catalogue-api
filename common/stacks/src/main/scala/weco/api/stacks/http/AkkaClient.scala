package weco.api.stacks.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{
  Authorization,
  BasicHttpCredentials,
  OAuth2BearerToken
}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import io.circe.generic.extras.JsonKey
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.api.http.TokenExchange

import java.time.Instant
import scala.concurrent.{ExecutionContextExecutor, Future}

trait AkkaClient {

  sealed trait Response[T] {
    val content: Option[T]
  }
  case class SuccessResponse[T](content: Option[T]) extends Response[T]
  case class FailureResponse[T](content: Option[T]) extends Response[T]

  implicit val system: ActorSystem

  implicit val ec: ExecutionContextExecutor = system.dispatcher

  protected val baseUri: Uri

  import Path._

  protected def buildUri(
    path: Path,
    params: Map[String, String] = Map.empty
  ): Uri =
    baseUri
      .withPath(baseUri.path ++ Slash(path))
      .withQuery(Query(params))
}

trait AkkaClientGet extends AkkaClient {
  protected def get[Out](
    path: Path,
    params: Map[String, String] = Map.empty,
    headers: List[HttpHeader] = Nil
  )(
    implicit decoder: Decoder[Out]
  ): Future[Response[Out]] = {
    implicit val um: Unmarshaller[HttpEntity, Out] =
      CirceMarshalling.fromDecoder[Out]

    for {
      response <- Http().singleRequest(
        HttpRequest(
          uri = buildUri(path, params),
          headers = headers
        )
      )

      result <- response.entity match {
        case e if e.isKnownEmpty() => Future.successful(None)
        case _                     => Unmarshal(response).to[Out].map(Some(_))
      }

    } yield
      response.status match {
        case r if r.isSuccess() => SuccessResponse(result)
        case _                  => FailureResponse(result)
      }
  }
}

trait AkkaClientPost extends AkkaClient {
  protected def post[In, Out](
    path: Path,
    body: Option[In] = None,
    params: Map[String, String] = Map.empty,
    headers: List[HttpHeader] = Nil
  )(
    implicit
    encoder: Encoder[In],
    decoder: Decoder[Out]
  ): Future[Response[Out]] = {
    implicit val m: Marshaller[In, RequestEntity] =
      CirceMarshalling.fromEncoder[In]

    implicit val um: Unmarshaller[HttpEntity, Out] =
      CirceMarshalling.fromDecoder[Out]

    for {
      entity <- body match {
        case Some(body) => Marshal(body).to[RequestEntity]
        case None       => Future.successful(HttpEntity.Empty)
      }

      response <- Http().singleRequest(
        HttpRequest(
          HttpMethods.POST,
          uri = buildUri(path, params),
          headers = headers,
          entity = entity
        )
      )

      result <- response.entity match {
        case e if e.isKnownEmpty() => Future.successful(None)
        case _                     => Unmarshal(response).to[Out].map(Some(_))
      }

    } yield
      response.status match {
        case r if r.isSuccess() => SuccessResponse(result)
        case _                  => FailureResponse(result)
      }
  }
}

trait AkkaClientTokenExchange
    extends AkkaClientPost
    with TokenExchange[BasicHttpCredentials, OAuth2BearerToken] {

  case class AccessToken(
    @JsonKey("access_token") accessToken: String,
    @JsonKey("expires_in") expiresIn: Int
  )

  val tokenPath: Path

  protected def getNewToken(
    credentials: BasicHttpCredentials
  ): Future[(OAuth2BearerToken, Instant)] =
    for {
      response <- post[String, AccessToken](
        path = tokenPath,
        headers = List(
          Authorization(
            credentials
          )
        )
      )

      result <- response match {
        case SuccessResponse(Some(AccessToken(access_token, expires_in))) =>
          Future.successful(
            (
              OAuth2BearerToken(access_token),
              Instant.now().plusSeconds(expires_in))
          )
        case _ =>
          Future.failed(
            new Exception(s"Failed to get access token.")
          )
      }
    } yield result
}
