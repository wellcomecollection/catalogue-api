package uk.ac.wellcome.platform.api.models

import java.time.{LocalDateTime, ZoneOffset}

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.parse.DateHelpers

sealed trait PeriodRange

object PeriodRange {
  object Decade extends PeriodRange
  object HalfCentury extends PeriodRange
  object Century extends PeriodRange
}

object DateAggregationMerger extends DateHelpers {

  import PeriodRange._

  /** Dynamically merges a set of date aggregation results based on the range
    *  of dates covered. Merging happens recursively until the number of buckets
    *  is no greater than maxBuckets, or we reach the broadest aggregation
    *  granularity (i.e. centuries)
    */
  def apply(agg: Aggregation[Period],
            maxBuckets: Int = 20,
            range: PeriodRange = Decade): Aggregation[Period] =
    if (agg.buckets.length > maxBuckets)
      range match {
        case Decade =>
          DateAggregationMerger(
            Aggregation(mergeBuckets(agg.buckets, 10)),
            maxBuckets,
            HalfCentury)
        case HalfCentury =>
          DateAggregationMerger(
            Aggregation(mergeBuckets(agg.buckets, 50)),
            maxBuckets,
            Century)
        case Century =>
          Aggregation(mergeBuckets(agg.buckets, 100))
      } else agg

  private def mergeBuckets(buckets: List[AggregationBucket[Period]],
                           yearRange: Int): List[AggregationBucket[Period]] =
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
            InstantRange(yearStart(startYear), yearEnd(endYear), label)
          AggregationBucket(Period(label, Some(range)), count)
      }

  private def yearFromPeriod(period: Period): Option[Int] =
    period.range.map { range =>
      LocalDateTime
        .ofInstant(range.from, ZoneOffset.UTC)
        .getYear
    }
}
