package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.searches.{
  MultiSearchRequest,
  SearchRequest,
  SearchResponse
}
import com.sksamuel.elastic4s.{Hit, Index, Response}
import grizzled.slf4j.Logging
import io.circe.Decoder
import weco.Tracing
import weco.api.search.elasticsearch.templateSearch.{
  TemplateSearchHandlers,
  TemplateSearchRequest
}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ElasticsearchService(elasticClient: ResilientElasticClient)(implicit
                                                         ec: ExecutionContext)
    extends Logging
    with Tracing
    with TemplateSearchHandlers {

  def findById[T](id: String)(
    index: Index
  )(implicit decoder: Decoder[T]): Future[Either[ElasticsearchError, T]] =
    for {
      response: Response[GetResponse] <- withActiveTrace(elasticClient.execute {
        get(index, id)
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
  )(implicit
    decoder: Decoder[T]): Future[Seq[Either[ElasticsearchError, Seq[T]]]] =
    for {
      multiSearchResults <- executeMultiSearchRequest(request)
      deserialisedResults = multiSearchResults.map {
        case Right(searchResponse) =>
          Right(searchResponse.hits.hits.map(deserialize[T]).toSeq)
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
      debug(s"Sending ES request: ${request.show}")
      val transaction = Tracing.currentTransaction
      withActiveTrace(elasticClient.execute(request))
        .map(_.toEither)
        .map {
          case Right(response) =>
            transaction.setLabel("elasticTook", response.took)
            Right(response)

          case Left(err) =>
            warn(s"Error while making request=${request.show}, error=$err")
            Left(ElasticsearchError(err))
        }
    }

  def executeTemplateSearchRequest(
    request: TemplateSearchRequest
  ): Future[Either[ElasticsearchError, SearchResponse]] =
    spanFuture(
      name = "ElasticSearch#executeTemplateSearchRequest",
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
            warn(s"Error while making request=${request.show}, error=$err")
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
      debug(s"Sending ES request: ${request.show}")
      val transaction = Tracing.currentTransaction
      withActiveTrace(elasticClient.execute(request))
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
}
