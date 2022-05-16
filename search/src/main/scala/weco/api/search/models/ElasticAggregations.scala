package weco.api.search.models

import com.sksamuel.elastic4s.requests.searches.aggs.responses.{Aggregations => Elastic4sAggregations}
import grizzled.slf4j.Logging
import io.circe.{Decoder, Json}

import scala.util.Failure

trait ElasticAggregations extends Logging {
  implicit class ThrowableEitherOps[T](either: Either[Throwable, T]) {
    def getMessage: Either[String, T] =
      either.left.map(_.getMessage)
  }

  implicit class EnhancedEsAggregations(aggregations: Elastic4sAggregations) {
    def decodeAgg[T: Decoder](name: String): Option[Aggregation[T]] =
      aggregations
        .getAgg(name)
        .flatMap(
          _.safeTo[Aggregation[T]](
            (json: String) => AggregationMapping.aggregationParser[T](json)
          ).recoverWith {
            case err =>
              warn("Failed to parse aggregation from ES", err)
              Failure(err)
          }.toOption
        )

    // Note: eventually this method will replace the decodeAgg method above, but we have
    // them both while images/works aggregations are handled differently.
    def decodeJsonAgg(name: String): Option[Aggregation[Json]] =
      aggregations
        .getAgg(name)
        .flatMap(
          _.safeTo[Aggregation[Json]](
            (json: String) => {
              AggregationMapping.jsonAggregationParse(json)
            }
          ).recoverWith {
            case err =>
              warn("Failed to parse aggregation from ES", err)
              println(s"Failed to parse aggregation from ES: $err")
              Failure(err)
          }.toOption
        )
  }
}
