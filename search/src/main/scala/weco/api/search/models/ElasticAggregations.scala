package weco.api.search.models

import com.sksamuel.elastic4s.requests.searches.aggs.responses.{
  Aggregations => Elastic4sAggregations
}
import grizzled.slf4j.Logging

import scala.util.Failure

trait ElasticAggregations extends Logging {
  implicit class ThrowableEitherOps[T](either: Either[Throwable, T]) {
    def getMessage: Either[String, T] =
      either.left.map(_.getMessage)
  }

  implicit class EnhancedEsAggregations(aggregations: Elastic4sAggregations) {
    def decodeAgg(name: String): Option[Aggregation] = {
      aggregations
        .getAgg(name)
        .flatMap(
          _.safeTo[Aggregation](
            (json: String) => {
              AggregationMapping.aggregationParser(json)
            }
          ).recoverWith {
            case err =>
              warn("Failed to parse aggregation from ES", err)
              Failure(err)
          }.toOption
        )
    }
  }
}
