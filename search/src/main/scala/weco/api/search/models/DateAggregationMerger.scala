package weco.api.search.models

import java.time.{LocalDate, LocalDateTime, Month, ZoneOffset}
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.work.{InstantRange, Period}

sealed trait DateBucketRange

object DateBucketRange {
  object Decade extends DateBucketRange
  object HalfCentury extends DateBucketRange
  object Century extends DateBucketRange
}

object DateAggregationMerger {

  import DateBucketRange._

  /** Dynamically merges a set of date aggregation results based on the range
    *  of dates covered. Merging happens recursively until the number of buckets
    *  is no greater than maxBuckets, or we reach the broadest aggregation
    *  granularity (i.e. centuries)
    */
  def apply(
    agg: Aggregation[Period[IdState.Minted]],
    maxBuckets: Int = 20,
    range: DateBucketRange = Decade
  ): Aggregation[Period[IdState.Minted]] =
    if (agg.buckets.length > maxBuckets)
      range match {
        case Decade =>
          DateAggregationMerger(
            Aggregation(mergeBuckets(agg.buckets, 10)),
            maxBuckets,
            HalfCentury
          )
        case HalfCentury =>
          DateAggregationMerger(
            Aggregation(mergeBuckets(agg.buckets, 50)),
            maxBuckets,
            Century
          )
        case Century =>
          Aggregation(mergeBuckets(agg.buckets, 100))
      } else agg

  private def mergeBuckets(
    buckets: List[AggregationBucket[Period[IdState.Minted]]],
    yearRange: Int
  ): List[AggregationBucket[Period[IdState.Minted]]] =
    buckets
      .foldLeft(Map.empty[Int, Int]) {
        case (map, bucket) =>
          yearFromPeriod(bucket.data) match {
            case Some(year) =>
              val key = year / yearRange
              map.updated(key, map.getOrElse(key, 0) + bucket.count)
            case None => map
          }
      }
      .toList
      .sortBy(_._1)
      .map {
        case (key, count) =>
          val startYear = key * yearRange
          val endYear = startYear + yearRange - 1
          val label = s"$startYear-$endYear"
          val range =
            InstantRange(
              from = LocalDate.of(startYear, Month.JANUARY, 1),
              to = LocalDate.of(endYear, Month.DECEMBER, 31),
              label = label
            )
          AggregationBucket(
            data = Period[IdState.Minted](
              id = IdState.Unidentifiable,
              label = label,
              range = Some(range)
            ),
            count = count
          )
      }

  private def yearFromPeriod(period: Period[IdState.Minted]): Option[Int] =
    period.range.map { range =>
      LocalDateTime
        .ofInstant(range.from, ZoneOffset.UTC)
        .getYear
    }
}