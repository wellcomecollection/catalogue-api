package weco.api.search.matchers

import com.sksamuel.elastic4s.requests.searches.aggs.{
  FilterAggregation,
  TermsAggregation
}
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher}
import weco.api.search.models.WorkFilter

trait AggregationRequestMatchers {
  trait MockFilter {
    val filter: WorkFilter
  }

  def filters(
    expectedFilters: Seq[WorkFilter]
  ): HavePropertyMatcher[FilterAggregation, Seq[WorkFilter]] =
    (left: FilterAggregation) => {
      val actualFilters = left.query
        .asInstanceOf[BoolQuery]
        .filters
        .asInstanceOf[Seq[MockFilter]]
        .map(_.filter)
      HavePropertyMatchResult(
        actualFilters == expectedFilters,
        "filters",
        expectedFilters,
        actualFilters
      )
    }

  def filter(
    expectedFilter: WorkFilter
  ): HavePropertyMatcher[FilterAggregation, WorkFilter] =
    (left: FilterAggregation) => {
      val actualFilter = left.query
        .asInstanceOf[MockFilter]
        .filter
      HavePropertyMatchResult(
        actualFilter == expectedFilter,
        "filter",
        expectedFilter,
        actualFilter
      )
    }

  def aggregationField(
    expectedField: String
  ): HavePropertyMatcher[FilterAggregation, String] =
    (left: FilterAggregation) => {
      val actualField =
        left.subaggs.head.asInstanceOf[TermsAggregation].field.get
      HavePropertyMatchResult(
        actualField == expectedField,
        "aggregationField",
        expectedField,
        actualField
      )
    }
}
