package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.searches.{SearchRequest, SearchResponse}
import com.sksamuel.elastic4s.{
  ElasticClient,
  ElasticError,
  Hit,
  Index,
  Response
}
import grizzled.slf4j.Logging
import io.circe.Decoder
import uk.ac.wellcome.Tracing
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ElasticsearchService(elasticClient: ElasticClient)(
  implicit ec: ExecutionContext
) extends Logging
    with Tracing {

  def findById[T](id: CanonicalId)(index: Index)(
    implicit decoder: Decoder[T]): Future[Either[ElasticError, Option[T]]] =
    for {
      response: Response[GetResponse] <- withActiveTrace(elasticClient.execute {
        get(index, id.underlying)
      })

      result = response.toEither match {
        case Right(getResponse) if getResponse.exists =>
          Right(Some(deserialize[T](getResponse)))

        case Right(_) => Right(None)

        case Left(err) => Left(err)
      }
    } yield result

  def findBySearch[T](request: SearchRequest)(
    implicit decoder: Decoder[T]): Future[Either[ElasticError, List[T]]] =
    for {
      response <- executeSearchRequest(request)

      results = response.map { searchResponse =>
        searchResponse.hits.hits
          .map(deserialize[T])
          .toList
      }
    } yield results

  def executeSearchRequest(
    request: SearchRequest): Future[Either[ElasticError, SearchResponse]] =
    spanFuture(
      name = "ElasticSearch#executeSearchRequest",
      spanType = "request",
      subType = "elastic",
      action = "query"
    ) {
      debug(s"Sending ES request: ${request.show}")
      val transaction = Tracing.currentTransaction
      withActiveTrace(elasticClient.execute(request))
        .map(_.toEither)
        .map { responseOrError =>
          responseOrError.map { res =>
            transaction.setLabel("elasticTook", res.took)
            res
          }
        }
    }

  private def deserialize[T](hit: Hit)(implicit decoder: Decoder[T]): T =
    hit.safeTo[T] match {
      case Success(work) => work
      case Failure(e) =>
        throw new RuntimeException(
          s"Unable to parse JSON($e): ${hit.sourceAsString}"
        )
    }
}
