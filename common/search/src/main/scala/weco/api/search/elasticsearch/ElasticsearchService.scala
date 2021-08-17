package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.searches.{MultiSearchRequest, SearchRequest, SearchResponse}
import com.sksamuel.elastic4s.{ElasticClient, Hit, Index, Response}
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
      response: Response[GetResponse] <- withActiveTrace(elasticClient.execute {
        get(index, id.underlying)
      })

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
  )(implicit decoder: Decoder[T]): Future[(Seq[ElasticsearchError], Seq[T])] =
    executeMultiSearchRequest(request).map {
      case (errors, responses) =>
        val deserialisedResponses = responses
          .flatMap(_.hits.hits)
          .map(deserialize[T])

        (errors, deserialisedResponses)
    }

  def executeSearchRequest(
    request: SearchRequest
  ): Future[Either[ElasticsearchError, SearchResponse]] =
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
  ): Future[(Seq[ElasticsearchError], Seq[SearchResponse])] =
    spanFuture(
      name = "ElasticSearch#executeMultiSearchRequest",
      spanType = "request",
      subType = "elastic",
      action = "query"
    ) {
      debug(s"Sending ES request: ${request.show}")
      val transaction = Tracing.currentTransaction
      withActiveTrace(elasticClient.execute(request))
        .map(_.toEither)
        .map {
          case Right(multiResponse) =>
            val foldInitial = (0L, Seq.empty[Long], Seq.empty[ElasticsearchError], Seq.empty[SearchResponse])

            val (finalTotalTimeTaken, finalTimesTaken, finalErrors, finalSearchResponses) =
              multiResponse.items.foldLeft(foldInitial) {
                (acc, item) =>
                  val (timeTakenTotal, timesTaken, errors, searchResponses) = acc

                  item.response match {
                    case Right(itemResponse) => {
                      val updatedTotalTimeTaken = timeTakenTotal + itemResponse.took
                      val updatedTimesTaken = timesTaken :+ itemResponse.took
                      val updatedSearchResponses = searchResponses :+ itemResponse

                      (updatedTotalTimeTaken, updatedTimesTaken, errors, updatedSearchResponses)
                    }

                    case Left(error) => {
                      val updatedErrors = errors :+ ElasticsearchError(error)
                      (timeTakenTotal, timesTaken, updatedErrors, searchResponses)
                    }
                  }
              }

            finalTimesTaken.zipWithIndex.map { case (timeTaken, index) =>
              transaction.setLabel(s"elasticTook-$index", timeTaken)
            }
            transaction.setLabel("elasticTookTotal", finalTotalTimeTaken)

            (finalErrors, finalSearchResponses)

          case Left(err) => (Seq(ElasticsearchError(err)), Seq.empty)
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
