package weco.api.search.models

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.generators.PeriodGenerators

class DateAggregationMergerTest extends AnyFunSpec with Matchers with PeriodGenerators {

  it("aggregates by decade when too many buckets") {
    val aggregation = Aggregation(
      List(
        AggregationBucket(createPeriodForYear("1954"), 2),
        AggregationBucket(createPeriodForYear("1958"), 1),
        AggregationBucket(createPeriodForYear("1960"), 1),
        AggregationBucket(createPeriodForYear("1969"), 5),
        AggregationBucket(createPeriodForYear("1982"), 4),
        AggregationBucket(createPeriodForYear("1983"), 2)
      )
    )
    DateAggregationMerger(aggregation, maxBuckets = 5) shouldBe Aggregation(
      List(
        AggregationBucket(createPeriodForYearRange("1950", "1959"), 3),
        AggregationBucket(createPeriodForYearRange("1960", "1969"), 6),
        AggregationBucket(createPeriodForYearRange("1980", "1989"), 6)
      )
    )
  }

  it("aggregates by half century when too many buckets") {
    val aggregation = Aggregation(
      List(
        AggregationBucket(createPeriodForYear("1898"), 2),
        AggregationBucket(createPeriodForYear("1900"), 1),
        AggregationBucket(createPeriodForYear("1940"), 1),
        AggregationBucket(createPeriodForYear("1958"), 5),
        AggregationBucket(createPeriodForYear("1960"), 1),
        AggregationBucket(createPeriodForYear("1969"), 5),
        AggregationBucket(createPeriodForYear("1982"), 4),
        AggregationBucket(createPeriodForYear("1983"), 2)
      )
    )
    DateAggregationMerger(aggregation, maxBuckets = 5) shouldBe Aggregation(
      List(
        AggregationBucket(createPeriodForYearRange("1850", "1899"), 2),
        AggregationBucket(createPeriodForYearRange("1900", "1949"), 2),
        AggregationBucket(createPeriodForYearRange("1950", "1999"), 17)
      )
    )
  }

  it("aggregates by century when too many buckets") {
    val aggregation = Aggregation(
      List(
        AggregationBucket(createPeriodForYear("1409"), 1),
        AggregationBucket(createPeriodForYear("1608"), 1),
        AggregationBucket(createPeriodForYear("1740"), 1),
        AggregationBucket(createPeriodForYear("1798"), 4),
        AggregationBucket(createPeriodForYear("1800"), 4),
        AggregationBucket(createPeriodForYear("1824"), 4),
        AggregationBucket(createPeriodForYear("1934"), 1),
        AggregationBucket(createPeriodForYear("2012"), 3)
      )
    )
    DateAggregationMerger(aggregation, maxBuckets = 5) shouldBe Aggregation(
      List(
        AggregationBucket(createPeriodForYearRange("1400", "1499"), 1),
        AggregationBucket(createPeriodForYearRange("1600", "1699"), 1),
        AggregationBucket(createPeriodForYearRange("1700", "1799"), 5),
        AggregationBucket(createPeriodForYearRange("1800", "1899"), 8),
        AggregationBucket(createPeriodForYearRange("1900", "1999"), 1),
        AggregationBucket(createPeriodForYearRange("2000", "2099"), 3)
      )
    )
  }
}
