package weco.api.search.models

import com.sksamuel.elastic4s.requests.searches.aggs.responses.{Aggregations => Elastic4sAggregations}
import grizzled.slf4j.Logging
import io.circe.Json
import weco.api.search.models.AggregationMapping.AggregationDecoder

import scala.util.Failure

trait ElasticAggregations extends Logging {
  implicit class ThrowableEitherOps[T](either: Either[Throwable, T]) {
    def getMessage: Either[String, T] =
      either.left.map(_.getMessage)
  }

  implicit class EnhancedEsAggregations(aggregations: Elastic4sAggregations) {
    def decodeAgg[T: AggregationDecoder](name: String): Option[Aggregation[T]] = {
      aggregations
        .getAgg(name)
        .flatMap(
          _.safeTo[Aggregation[T]](
            (json: String) => {
              AggregationMapping.aggregationParser[T](json)
            }
          ).recoverWith {
            case err =>
              warn("Failed to parse aggregation from ES", err)
              Failure(err)
          }.toOption
        )
    }

    def decodeJsonAgg(name: String): Option[Aggregation[Json]] = {
      decodeAgg[Json](name)
    }
  }
}
