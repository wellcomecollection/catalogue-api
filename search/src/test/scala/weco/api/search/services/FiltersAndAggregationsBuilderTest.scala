package weco.api.search.services

import com.sksamuel.elastic4s.requests.searches.aggs.{
  AbstractAggregation,
  Aggregation,
  FilterAggregation,
  GlobalAggregation
}
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.models._
import weco.api.search.models.{
  FormatFilter,
  GenreFilter,
  LanguagesFilter,
  WorkFilter
}
import weco.catalogue.display_model.models.WorkAggregationRequest

class FiltersAndAggregationsBuilderTest extends AnyFunSpec with Matchers {

  describe("aggregation-level filtering") {
    it("applies to aggregations with a paired filter") {
      val formatFilter = FormatFilter(Seq("bananas"))
      val languagesFilter = LanguagesFilter(Seq("en"))
      val builder = new WorkFiltersAndAggregationsBuilder(
        aggregationRequests =
          List(WorkAggregationRequest.Format, WorkAggregationRequest.Languages),
        filters = List(formatFilter, languagesFilter),
        requestToAggregation = requestToAggregation,
        filterToQuery = filterToQuery,
        searchQuery = MockSearchQuery
      )

      builder.filteredAggregations should have length 2
      builder.filteredAggregations.head shouldBe a[GlobalAggregation]

      val topAgg = builder.filteredAggregations.head
        .asInstanceOf[GlobalAggregation]
        .subaggs
        .head
      topAgg shouldBe a[MockAggregation]

      val agg = topAgg.asInstanceOf[MockAggregation]
      agg.subaggs.head shouldBe a[FilterAggregation]
      agg.request shouldBe WorkAggregationRequest.Format
    }

    it("does not apply to aggregations without a paired filter") {
      val languagesFilter = LanguagesFilter(Seq("en"))
      val builder = new WorkFiltersAndAggregationsBuilder(
        aggregationRequests = List(WorkAggregationRequest.Format),
        filters = List(languagesFilter),
        requestToAggregation = requestToAggregation,
        filterToQuery = filterToQuery,
        searchQuery = MockSearchQuery
      )

      builder.filteredAggregations should have length 1
      builder.filteredAggregations.head shouldBe a[MockAggregation]
      builder.filteredAggregations.head
        .asInstanceOf[MockAggregation]
        .subaggs should have length 0
    }

    it("applies paired filters to non-paired aggregations") {
      val formatFilter = FormatFilter(Seq("bananas"))
      val builder = new WorkFiltersAndAggregationsBuilder(
        aggregationRequests =
          List(WorkAggregationRequest.Format, WorkAggregationRequest.Languages),
        filters = List(formatFilter),
        requestToAggregation = requestToAggregation,
        filterToQuery = filterToQuery,
        searchQuery = MockSearchQuery
      )

      builder.filteredAggregations should have length 2
      builder.filteredAggregations.head shouldBe a[GlobalAggregation]
      builder.filteredAggregations.head
        .asInstanceOf[GlobalAggregation]
        .subaggs
        .head
        .asInstanceOf[MockAggregation]
        .subaggs should have length 1

      builder.filteredAggregations(1) shouldBe a[MockAggregation]
      builder
        .filteredAggregations(1)
        .asInstanceOf[MockAggregation]
        .subaggs should have length 0
    }

    it("applies all other aggregation-dependent filters to the paired filter") {
      val formatFilter = FormatFilter(Seq("bananas"))
      val languagesFilter = LanguagesFilter(Seq("en"))
      val genreFilter = GenreFilter(Seq("durian"))
      val builder = new WorkFiltersAndAggregationsBuilder(
        aggregationRequests = List(
          WorkAggregationRequest.Format,
          WorkAggregationRequest.Languages,
          WorkAggregationRequest.Genre
        ),
        filters = List(formatFilter, languagesFilter, genreFilter),
        requestToAggregation = requestToAggregation,
        filterToQuery = filterToQuery,
        searchQuery = MockSearchQuery
      )

      val agg =
        builder.filteredAggregations.head
          .asInstanceOf[GlobalAggregation]
          .subaggs
          .head
          .asInstanceOf[MockAggregation]
          .subaggs
          .head
          .asInstanceOf[FilterAggregation]
      agg.query shouldBe a[BoolQuery]
      val query = agg.query.asInstanceOf[BoolQuery]
      query.filters should not contain MockQuery(formatFilter)
      query.filters should contain only (
        MockSearchQuery, MockQuery(languagesFilter), MockQuery(genreFilter)
      )
    }
  }

  private def requestToAggregation(
    request: WorkAggregationRequest
  ): Aggregation =
    MockAggregation("cabbage", request)

  private def filterToQuery(filter: WorkFilter): Query = MockQuery(filter)

  private case class MockQuery(filter: WorkFilter) extends Query
  private case object MockSearchQuery extends Query

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
