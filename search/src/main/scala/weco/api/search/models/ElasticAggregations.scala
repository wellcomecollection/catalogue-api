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
    def decodeAgg(name: String): Option[Aggregation] = {
      for {
        aggJson1 <- aggregations.getAgg(name)
        aggJson2 <-  aggregations.getAgg(name + "Global")
        parsedAggregation <- aggJson1
          .safeTo[Aggregation] { json =>
            aggJson2.safeTo[Aggregation] { json2 =>
              AggregationMapping.aggregationParser(json, json2)
            }
          }
          .toOption
      } yield parsedAggregation
    }
  }
}
