package weco.api.search.matchers

import com.sksamuel.elastic4s.requests.searches.aggs.{FilterAggregation, NestedAggregation, TermsAggregation}
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher}

trait AggregationRequestMatchers {
  def filters(
    expectedFilters: Seq[Query]
  ): HavePropertyMatcher[FilterAggregation, Seq[Query]] =
    (left: FilterAggregation) => {
      val actualFilters = left.query.asInstanceOf[BoolQuery].filters

      HavePropertyMatchResult(
        actualFilters == expectedFilters,
        "filters",
        expectedFilters,
        actualFilters
      )
    }

  def filter(
    expectedFilter: Query
  ): HavePropertyMatcher[FilterAggregation, Query] =
    (left: FilterAggregation) => {
      val actualFilter = left.query

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
        left.subaggs.head.asInstanceOf[NestedAggregation].subaggs.head.asInstanceOf[TermsAggregation].field.get

      HavePropertyMatchResult(
        actualField == expectedField,
        "aggregationField",
        expectedField,
        actualField
      )
    }
}
