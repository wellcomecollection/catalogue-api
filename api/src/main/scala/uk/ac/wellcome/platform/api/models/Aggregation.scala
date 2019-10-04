package uk.ac.wellcome.platform.api.models

import scala.util.Try
import io.circe.Decoder
import java.time.{Instant, LocalDateTime, ZoneOffset}

import grizzled.slf4j.Logging
import uk.ac.wellcome.models.work.internal.{
  AbstractConcept,
  Displayable,
  Genre,
  Period,
  WorkType
}
import uk.ac.wellcome.json.JsonUtil._

case class AggregatedGenre(label: String)

case class Aggregations(
  workType: Option[Aggregation[WorkType]] = None,
  genres: Option[Aggregation[Genre[Displayable[AbstractConcept]]]] = None,
  productionDates: Option[Aggregation[Period]] = None)

object Aggregations extends Logging {

  def apply(jsonString: String): Option[Aggregations] =
    fromJson[EsAggregations](jsonString)
      .collect {
        case EsAggregations(workType, aggregatedGenres, date)
            if List(workType, aggregatedGenres, date).flatten.nonEmpty => {

          // We have to do this conversion here as we only get a label back from the Elastic response
          // as it creating a composite aggregation, which doesn't really have the knowledge of a full
          // Genre object
          val genres = convert(
            getAggregation[AggregatedGenre](aggregatedGenres),
            (aggregateGenre: AggregatedGenre) =>
              Genre[Displayable[AbstractConcept]](aggregateGenre.label, List()))

          Some(
            Aggregations(
              workType = getAggregation[WorkType](workType),
              genres = genres,
              productionDates = getAggregation[Period](date)
            )
          )
        }
      }
      .getOrElse { None }

  def convert[T, V](maybeAgg: Option[Aggregation[T]],
                    c: T => V): Option[Aggregation[V]] =
    maybeAgg.map { agg =>
      Aggregation(
        agg.buckets.map { aggBucket =>
          AggregationBucket(c(aggBucket.data), aggBucket.count)
        }
      )
    }

  def getAggregation[T](
    maybeEsAgg: Option[EsAggregation[T]]): Option[Aggregation[T]] =
    maybeEsAgg.map { esAgg =>
      Aggregation(
        esAgg.buckets.map { esAggBucket =>
          AggregationBucket(esAggBucket.key, esAggBucket.doc_count)
        }
      )
    }

  // Elasticsearch encodes the date key as milliseconds since the epoch
  implicit val decodePeriod: Decoder[Period] =
    Decoder.decodeLong.emap { epochMilli =>
      Try { Instant.ofEpochMilli(epochMilli) }
        .map { instant =>
          LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
        }
        .map { date =>
          Right(Period(date.getYear.toString))
        }
        .getOrElse { Left("Error decoding") }
    }
}

case class Aggregation[T](buckets: List[AggregationBucket[T]])
case class AggregationBucket[T](data: T, count: Int)

/**
  * We use these to convert the JSON into Elasticsearch case classes (not supplied via elastic4s)
  * and then convert them into our representations of aggregations.
  * This is to get around things like having property names like `doc_count` and `key`
  * The general format here is:
  *
  * {
  *   "buckets": {
  *     "key": {
  *       "this": "is",
  *       "structured": "content"
  *     },
  *     "doc_count" 1009
  *   }
  * }
  *
  * And we convert them to:
  * {
  *   "buckets": {
  *     "data": {
  *       "this": "is",
  *       "structured": "content"
  *     },
  *     "count" 1009
  *   }
  * }
  */
case class EsAggregations(
  workType: Option[EsAggregation[WorkType]] = None,
  genres: Option[EsAggregation[AggregatedGenre]] = None,
  productionDates: Option[EsAggregation[Period]] = None,
)
case class EsAggregation[T](buckets: List[EsAggregationBucket[T]])
case class EsAggregationBucket[T](key: T, doc_count: Int)
