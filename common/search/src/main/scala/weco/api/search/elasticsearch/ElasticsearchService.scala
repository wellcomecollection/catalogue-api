package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.JavaClientExceptionWrapper
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.searches.{
  MultiSearchRequest,
  SearchRequest,
  SearchResponse
}
import com.sksamuel.elastic4s.{ElasticClient, Handler, Hit, Index, Response}
import grizzled.slf4j.Logging
import io.circe.Decoder
import weco.Tracing
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ElasticsearchService(elasticClient: ElasticClient)(
  implicit ec: ExecutionContext
) extends Logging
    with Tracing {

  def findById[T](id: CanonicalId)(
    index: Index
  )(implicit decoder: Decoder[T]): Future[Either[ElasticsearchError, T]] =
    for {
      response: Response[GetResponse] <- executeRequest {
        get(index, id.underlying)
      }

      result = response.toEither match {
        case Right(getResponse) if getResponse.exists =>
          Right(deserialize[T](getResponse))

        case Right(_) => Left(DocumentNotFoundError(id))

        case Left(err) => Left(ElasticsearchError(err))
      }
    } yield result

  def findBySearch[T](
    request: SearchRequest
  )(implicit decoder: Decoder[T]): Future[Either[ElasticsearchError, List[T]]] =
    for {
      response <- executeSearchRequest(request)

      results = response.map { searchResponse =>
        searchResponse.hits.hits
          .map(deserialize[T])
          .toList
      }
    } yield results

  def findByMultiSearch[T](
    request: MultiSearchRequest
  )(
    implicit decoder: Decoder[T]
  ): Future[Seq[Either[ElasticsearchError, Seq[T]]]] =
    for {
      multiSearchResults <- executeMultiSearchRequest(request)
      deserialisedResults = multiSearchResults.map {
        case Right(searchResponse) => {
          Right(searchResponse.hits.hits.map(deserialize[T]).toSeq)
        }
        case Left(err) => Left(err)
      }
    } yield deserialisedResults

  def executeSearchRequest(
    request: SearchRequest
  ): Future[Either[ElasticsearchError, SearchResponse]] =
    spanFuture(
      name = "ElasticSearch#executeSearchRequest",
      spanType = "request",
      subType = "elastic",
      action = "query"
    ) {
      val transaction = Tracing.currentTransaction
      executeRequest(request)
        .map(_.toEither)
        .map {
          case Right(response) =>
            transaction.setLabel("elasticTook", response.took)
            Right(response)

          case Left(err) =>
            Left(ElasticsearchError(err))
        }
    }

  def executeMultiSearchRequest(
    request: MultiSearchRequest
  ): Future[Seq[Either[ElasticsearchError, SearchResponse]]] =
    spanFuture(
      name = "ElasticSearch#executeMultiSearchRequest",
      spanType = "request",
      subType = "elastic",
      action = "query"
    ) {
      val transaction = Tracing.currentTransaction
      executeRequest(request)
        .map(_.toEither)
        .map {
          case Left(err) => throw err.asException
          case Right(multiResponse) =>
            val foldInitial = (
              0L,
              Seq.empty[Long],
              Seq.empty[Either[ElasticsearchError, SearchResponse]]
            )

            val (
              finalTotalTimeTaken,
              finalTimesTaken,
              finalResults
            ) =
              multiResponse.items.foldLeft(foldInitial) { (acc, item) =>
                val (timeTakenTotal, timesTaken, results) = acc

                item.response match {
                  case Right(itemResponse) =>
                    (
                      timeTakenTotal + itemResponse.took,
                      timesTaken :+ itemResponse.took,
                      results :+ Right(itemResponse)
                    )

                  case Left(error) =>
                    (
                      timeTakenTotal,
                      timesTaken,
                      results :+ Left(ElasticsearchError(error))
                    )
                }
              }

            finalTimesTaken.zipWithIndex.map {
              case (timeTaken, index) =>
                transaction.setLabel(s"elasticTook-$index", timeTaken)
            }
            transaction.setLabel("elasticTookTotal", finalTotalTimeTaken)

            finalResults
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

  // We occasionally see HTTP 500 errors from the search API, and looking in the
  // logs you see a timeout error:
  //
  //      com.sksamuel.elastic4s.http.JavaClientExceptionWrapper: java.net.ConnectException:
  //      Timeout connecting to [{URL of Elasticsearch cluster}]
  //
  // There's nothing particularly unusual about the failing request, and it resolves
  // on a reload -- the cluster just had a momentary flake.  Reloading the page should
  // fix the error, but it's not a great experience for the user.
  //
  // This code will retry a failing request if:
  //
  //    - it looks like one of these timeouts
  //    - if this is the first error we've seen on this request
  //
  // This retrying is deliberately conservative: if the cluster is down or this request
  // causes some persistent failure, we don't want to keep trying the cluster.

  import FutureRetryOps._

  private def isRetryable(t: Throwable): Boolean =
    t match {
      case JavaClientExceptionWrapper(exc: java.net.ConnectException)
          if exc.getMessage.startsWith("Timeout connecting to") =>
        true

      case _ => false
    }

  private def executeRequest[Request, U](request: Request)(
    implicit
    handler: Handler[Request, U],
    manifest: Manifest[U]
  ): Future[Response[U]] = {
    debug(s"Sending ES request: ${request.show}")

    val retryableFunction =
      ((r: Request) => withActiveTrace(elasticClient.execute(r)))
        .retry(maxAttempts = 2, isRetryable = isRetryable)

    retryableFunction(request)
  }
}
