package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.{ElasticClient, ElasticError, Hit, Index}
import io.circe.Decoder
import uk.ac.wellcome.Tracing
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ElasticLookup[T](
  elasticClient: ElasticClient
)(
  implicit
  ec: ExecutionContext,
  decoder: Decoder[T]
) extends Tracing {

  def lookupById(canonicalId: CanonicalId)(index: Index): Future[Either[ElasticError, Option[T]]] =
    executeGet(canonicalId)(index)
      .map {
        case Left(elasticError) => Left(elasticError)

        case Right(response) if response.exists => Right(Some(deserialize(response)))

        case Right(_) => Right(None)
      }

  private def executeGet(canonicalId: CanonicalId)(
    index: Index): Future[Either[ElasticError, GetResponse]] =
    withActiveTrace(elasticClient.execute {
      get(index, canonicalId.underlying)
    }).map { _.toEither }

  private def deserialize(hit: Hit): T =
    hit.safeTo[T] match {
      case Success(work) => work
      case Failure(e) =>
        throw new RuntimeException(
          s"Unable to parse JSON($e): ${hit.sourceAsString}"
        )
    }
}
