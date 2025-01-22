package weco.api.search.models

import com.sksamuel.elastic4s.requests.searches.aggs.responses.{
  Aggregations => Elastic4sAggregations
}
import grizzled.slf4j.Logging

trait ElasticAggregations extends Logging {
  implicit class ThrowableEitherOps[T](either: Either[Throwable, T]) {
    def getMessage: Either[String, T] =
      either.left.map(_.getMessage)
  }

  implicit class EnhancedEsAggregations(aggregations: Elastic4sAggregations) {
    def decodeAgg(name: String): Option[Aggregation] =
      for {
        filteredAggregation <- aggregations.getAgg(name)
        globalAggregation <- aggregations.getAgg(name + "Global")
        parsedAggregation <- filteredAggregation
          .safeTo[Aggregation] { filteredJson =>
            globalAggregation.safeTo[Aggregation] { globalJson =>
              AggregationMapping.aggregationParser(filteredJson, globalJson)
            }
          }
          .toOption
      } yield parsedAggregation
  }
}
