package weco.api.search.services

import com.sksamuel.elastic4s.requests.searches.aggs.{
  AbstractAggregation,
  Aggregation,
  FilterAggregation
}
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
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
    with TableDrivenPropertyChecks {

  describe("aggregation-level filtering") {
    it("applies to aggregations with a paired filter") {
      val formatFilter = FormatFilter(Seq("bananas"))
      val languagesFilter = LanguagesFilter(Seq("en"))
      val builder = new WorkFiltersAndAggregationsBuilder(
        aggregationRequests =
          List(WorkAggregationRequest.Format, WorkAggregationRequest.Languages),
        filters = List(formatFilter, languagesFilter),
        requestToAggregation = requestToAggregation,
        filterToQuery = filterToQuery
      )

      builder.filteredAggregations should have length 2

      builder.filteredAggregations.head shouldBe a[FilterAggregation]
      // The first aggregation is Format
      val filterAgg = builder.filteredAggregations.head
        .asInstanceOf[FilterAggregation]
      // Filtered on Language=en
      filterAgg.query
        .asInstanceOf[BoolQuery]
        .filters
        .head shouldBe MockQuery(LanguagesFilter(Seq("en")))
      // Within that filtered aggregation is the
      // aggregation for format.
      val formatAgg = filterAgg.subaggs.head
      formatAgg
        .asInstanceOf[MockAggregation]
        .request shouldBe WorkAggregationRequest.Format
      //Then comes the self aggregation
      val selfAgg = filterAgg.subaggs(1).asInstanceOf[FilterAggregation]
      // Which is the same aggregation
      selfAgg.subaggs.head
        .asInstanceOf[MockAggregation]
        .request shouldBe WorkAggregationRequest.Format
      //but additionally, it matches the filter on this field.
      selfAgg.query
        .asInstanceOf[MockQuery]
        .filter shouldBe FormatFilter(Seq("bananas"))
    }

    it("does not apply to aggregations without a paired filter") {
      val languagesFilter = LanguagesFilter(Seq("en"))
      val builder = new WorkFiltersAndAggregationsBuilder(
        aggregationRequests = List(WorkAggregationRequest.Format),
        filters = List(languagesFilter),
        requestToAggregation = requestToAggregation,
        filterToQuery = filterToQuery
      )

      builder.filteredAggregations should have length 1
      builder.filteredAggregations.head
        .asInstanceOf[FilterAggregation]
        .subaggs should have length 1 // not 2 - the second is the self aggregation
    }

    it("applies paired filters to non-paired aggregations") {
      val formatFilter = FormatFilter(Seq("bananas"))
      val builder = new WorkFiltersAndAggregationsBuilder(
        aggregationRequests =
          List(WorkAggregationRequest.Format, WorkAggregationRequest.Languages),
        filters = List(formatFilter),
        requestToAggregation = requestToAggregation,
        filterToQuery = filterToQuery
      )

      builder.filteredAggregations should have length 2
      //The first aggregation is Format, which has already been explored
      // in the "applies to aggregations with a paired filter" test
      builder.filteredAggregations.head shouldBe a[FilterAggregation]

      //The second aggregation is Language, with no corresponding
      // filter in this query
      builder
        .filteredAggregations(1)
        .asInstanceOf[FilterAggregation]
        .subaggs should have length 1
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
        requestToAggregation = requestToAggregation,
        filterToQuery = filterToQuery
      )

      builder.filteredAggregations should have length 3

      val t = Table(
        ("agg", "matchingFilter"),
        builder.filteredAggregations.zip(
          filters
        ): _*
      )

      forAll(t) { (agg: AbstractAggregation, thisFilter: WorkFilter) =>
        val filterQuery = agg
          .asInstanceOf[FilterAggregation]
          .query
          .asInstanceOf[BoolQuery]
        //Three filters are requested, each aggregation should
        // have only two.  i.e. not it's own
        filterQuery.filters should have length 2
        filterQuery.filters.map(_.asInstanceOf[MockQuery].filter) should contain theSameElementsAs filters
          .filterNot(_ == thisFilter)
      }
    }
  }

  private def requestToAggregation(
    request: WorkAggregationRequest
  ): Aggregation =
    MockAggregation("cabbage", request)

  private def filterToQuery(filter: WorkFilter): Query = MockQuery(filter)

  private case class MockQuery(filter: WorkFilter) extends Query
  // private case object MockSearchQuery extends Query

  private case class MockAggregation(
    name: String,
    request: WorkAggregationRequest,
    subaggs: Seq[AbstractAggregation] = Nil,
    metadata: Map[String, AnyRef] = Map.empty
  ) extends Aggregation {
    type T = MockAggregation
    override def subAggregations(aggs: Iterable[AbstractAggregation]): T =
      copy(subaggs = aggs.toSeq)
    override def metadata(map: Map[String, AnyRef]): T = copy(metadata = map)
  }
}
