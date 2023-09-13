package weco.api.search.services

import com.sksamuel.elastic4s.requests.searches.aggs.{
  AbstractAggregation,
  Aggregation,
  FilterAggregation,
  TermsAggregation
}
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.term.TermsQuery
import org.scalatest.{Inside, LoneElement}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import weco.api.search.matchers.AggregationRequestMatchers
import weco.api.search.models._
import weco.api.search.models.request.WorkAggregationRequest
import weco.api.search.models.{
  FormatFilter,
  GenreFilter,
  LanguagesFilter,
  WorkFilter
}

class FiltersAndAggregationsBuilderTest
    extends AnyFunSpec
    with Matchers
    with TableDrivenPropertyChecks
    with LoneElement
    with Inside
    with AggregationRequestMatchers {

  describe("aggregation-level filtering") {
    it("applies to aggregations with a paired filter") {
      val formatFilter = FormatFilter(Seq("bananas"))
      val languagesFilter = LanguagesFilter(Seq("en"))
      val builder = new WorkFiltersAndAggregationsBuilder(
        aggregationRequests =
          List(WorkAggregationRequest.Format, WorkAggregationRequest.Languages),
        filters = List(formatFilter, languagesFilter),
        requestToAggregation = requestToTermsAggregation,
        filterToQuery = filterToTermsQuery
      )

      // The mock aggregation generator stringifies the request object
      // into the field, just to show that it's choosing the right one.
      // This saves the tests having to replicate all the field mappings.
      val formatFieldName = WorkAggregationRequest.Format.toString
      val languagesFieldName = WorkAggregationRequest.Languages.toString

      builder.filteredAggregations should have length 2
      val firstAgg =
        builder.filteredAggregations.head.asInstanceOf[FilterAggregation]
      // The first aggregation is the Format one, filtered with the languages filter.
      firstAgg should have(
        filters(Seq(languagesFilter)),
        aggregationField(formatFieldName)
      )

      inside(firstAgg.subaggs(1)) {
        case selfFilter: FilterAggregation =>
          selfFilter should have(
            filter(formatFilter),
            aggregationField(formatFieldName)
          )
      }

      val secondAgg =
        builder.filteredAggregations(1).asInstanceOf[FilterAggregation]
      // The second aggregation is the Languages one, filtered with the languages filter.
      secondAgg should have(
        filters(Seq(formatFilter)),
        aggregationField(languagesFieldName)
      )

      inside(secondAgg.subaggs(1)) {
        case selfFilter: FilterAggregation =>
          selfFilter should have(
            filter(languagesFilter),
            aggregationField(languagesFieldName)
          )
      }
    }

    it("does not apply to aggregations without a paired filter") {
      val languagesFilter = LanguagesFilter(Seq("en"))
      val builder = new WorkFiltersAndAggregationsBuilder(
        aggregationRequests = List(WorkAggregationRequest.Format),
        filters = List(languagesFilter),
        requestToAggregation = requestToTermsAggregation,
        filterToQuery = filterToTermsQuery
      )
      // The aggregation list is just the requested aggregation
      // filtered by the requested (unpaired) filter.
      builder.filteredAggregations.loneElement
        .asInstanceOf[FilterAggregation]
        .subaggs
        .loneElement // This marks the absence of the "self" filteraggregation
        .asInstanceOf[TermsAggregation]
        .field
        .get shouldBe WorkAggregationRequest.Format.toString
    }

    it("applies paired filters to non-paired aggregations") {
      val formatFilter = FormatFilter(Seq("bananas"))
      val builder = new WorkFiltersAndAggregationsBuilder(
        aggregationRequests =
          List(WorkAggregationRequest.Format, WorkAggregationRequest.Languages),
        filters = List(formatFilter),
        requestToAggregation = requestToTermsAggregation,
        filterToQuery = filterToTermsQuery
      )

      builder.filteredAggregations should have length 2
      //The first aggregation is Format, which has a "self" subaggregation.
      // The details of the content of this aggregation are examined
      // in the "applies to aggregations with a paired filter" test.
      val firstAgg = builder.filteredAggregations.head
        .asInstanceOf[FilterAggregation]

      firstAgg.subaggs should have length 2
      firstAgg should have(
        filters(Nil),
        aggregationField(WorkAggregationRequest.Format.toString)
      )

      //The second aggregation is Language, which has no corresponding
      // filter in this query, so does not have a "self" subaggregation
      val secondAgg = builder
        .filteredAggregations(1)
        .asInstanceOf[FilterAggregation]

      secondAgg.subaggs should have length 1

      // But it should still be filtered using the format filter
      secondAgg should have(
        filters(Seq(formatFilter)),
        aggregationField(WorkAggregationRequest.Languages.toString)
      )

    }

    it("applies all other aggregation-dependent filters to the paired filter") {
      val formatFilter = FormatFilter(Seq("bananas"))
      val languagesFilter = LanguagesFilter(Seq("en"))
      val genreFilter = GenreFilter(Seq("durian"))
      val filters = List(formatFilter, languagesFilter, genreFilter)
      val builder = new WorkFiltersAndAggregationsBuilder(
        aggregationRequests = List(
          WorkAggregationRequest.Format,
          WorkAggregationRequest.Languages,
          WorkAggregationRequest.Genre
        ),
        filters = filters,
        requestToAggregation = requestToTermsAggregation,
        filterToQuery = filterToTermsQuery
      )

      builder.filteredAggregations should have length 3

      forAll(
        Table(
          ("agg", "matchingFilter"),
          // The aggregations and the filter list are expected to be
          // in the same order, so zipping them should result in
          // each aggregation being paired with its corresponding filter
          builder.filteredAggregations.zip(
            filters
          ): _*
        )
      ) { (agg: AbstractAggregation, thisFilter: WorkFilter) =>
        val filterQuery = agg
          .asInstanceOf[FilterAggregation]
          .query
          .asInstanceOf[BoolQuery]
        //Three filters are requested, each aggregation should
        // have only two.  i.e. not it's own
        filterQuery.filters should have length 2
        // And this ensures that it is the correct two.
        filterQuery.filters.map(_.asInstanceOf[MockFilter].filter) should contain theSameElementsAs filters
          .filterNot(_ == thisFilter)
      }
    }
  }

  private def requestToTermsAggregation(
    request: WorkAggregationRequest
  ): Aggregation =
    TermsAggregation(name = "cabbage", field = Some(request.toString))

  private def filterToTermsQuery(filter: WorkFilter): Query =
    new MockTermsQuery(filter)
  private class MockTermsQuery(val filter: WorkFilter)
      extends TermsQuery(field = "", values = Nil)
      with MockFilter
}
