package weco.api.search.services

import com.sksamuel.elastic4s.ElasticDsl.termsQuery
import com.sksamuel.elastic4s.requests.searches.aggs.{FilterAggregation, NestedAggregation, TermsAggregation}
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import org.scalatest.{Inside, LoneElement}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import weco.api.search.matchers.AggregationRequestMatchers
import weco.api.search.models.request.WorkAggregationRequest
import weco.api.search.models.{FormatFilter, GenreFilter, LanguagesFilter, WorkFilter}
import weco.api.search.services.WorksRequestBuilder.buildWorkFilterQuery

class AggregationsBuilderTest
    extends AnyFunSpec
    with Matchers
    with TableDrivenPropertyChecks
    with LoneElement
    with Inside
    with AggregationRequestMatchers {

  private val formatFieldName = "aggregatableValues.workType"
  private val languagesFieldName = "aggregatableValues.languages"
  private val formatFilterQuery = termsQuery(field = "filterableValues.format.id", values = Seq("bananas"))
  private val languagesFilterQuery = termsQuery(field = "filterableValues.languages.id", values = Seq("en"))


  describe("aggregation-level filtering") {
    it("applies to aggregations with a paired filter") {
      val formatFilter = FormatFilter(Seq("bananas"))
      val languagesFilter = LanguagesFilter(Seq("en"))

      val aggregations = WorksAggregationsBuilder.getAggregations(
        List(formatFilter, languagesFilter),
        List(WorkAggregationRequest.Format, WorkAggregationRequest.Languages)
      )

      aggregations should have length 2

      val firstNestedAggregation = aggregations.head.subaggs.head.asInstanceOf[NestedAggregation]
      val firstSelfNestedAggregation = aggregations.head.subaggs(1).asInstanceOf[NestedAggregation]

      // The first aggregation is the Format one, filtered with the languages filter.
      aggregations.head should have(
        filters(Seq(languagesFilterQuery)),
      )
      firstNestedAggregation.subaggs.head.asInstanceOf[TermsAggregation] should have(
        aggregationField(s"${formatFieldName}.id")
      )

      inside(firstSelfNestedAggregation.subaggs.head) {
        case selfFilter: TermsAggregation =>
          selfFilter should have(
            aggregationField(s"${formatFieldName}.id")
          )
      }

      val secondNestedAggregation = aggregations(1).subaggs.head.asInstanceOf[NestedAggregation]
      val secondSelfNestedAggregation = aggregations(1).subaggs(1).asInstanceOf[NestedAggregation]


      // The second aggregation is the Languages one, filtered with the format filter.
      aggregations(1) should have(
        filters(Seq(formatFilterQuery)),
      )
      secondNestedAggregation.subaggs.head.asInstanceOf[TermsAggregation] should have(
        aggregationField(s"${languagesFieldName}.id")
      )

      inside(secondSelfNestedAggregation.subaggs.head) { case selfFilter: TermsAggregation =>
        selfFilter should have(
          aggregationField(s"${languagesFieldName}.id")
        )
      }
    }

    it("does not apply to aggregations without a paired filter") {
      val languagesFilter = LanguagesFilter(Seq("en"))

      val aggregations = WorksAggregationsBuilder.getAggregations(
        List(languagesFilter),
        List(WorkAggregationRequest.Format)
      )

      // The aggregation list is just the requested aggregation
      // filtered by the requested (unpaired) filter.
      val filterAggregation = aggregations.loneElement
      val nestedAggregation = filterAggregation.subaggs.loneElement.asInstanceOf[NestedAggregation] // This marks the absence of the "self" aggregation
      val termsAggregation = nestedAggregation.subaggs.loneElement.asInstanceOf[TermsAggregation]

      termsAggregation.field.get shouldBe s"${formatFieldName}.id"
    }

    it("includes a label-based subaggregation") {
      val languagesFilter = LanguagesFilter(Seq("en"))

      val aggregations = WorksAggregationsBuilder.getAggregations(
        List(languagesFilter),
        List(WorkAggregationRequest.Format)
      )

      // The aggregation list is just the requested aggregation
      // filtered by the requested (unpaired) filter.
      val filterAggregation = aggregations.loneElement
      val nestedAggregation = filterAggregation.subaggs.loneElement.asInstanceOf[NestedAggregation] // This marks the absence of the "self" aggregation
      val termsAggregation = nestedAggregation.subaggs.loneElement.asInstanceOf[TermsAggregation]

      val labelSubaggregation = termsAggregation.subaggs.loneElement.asInstanceOf[TermsAggregation]

      labelSubaggregation.field.get shouldBe s"${formatFieldName}.label"
      labelSubaggregation.size shouldBe Some(1)
    }


    it("applies paired filters to non-paired aggregations") {
      val formatFilter = FormatFilter(Seq("bananas"))

      val aggregations = WorksAggregationsBuilder.getAggregations(
        List(formatFilter),
        List(WorkAggregationRequest.Format, WorkAggregationRequest.Languages)
      )

      aggregations should have length 2
      //The first aggregation is Format, which has a "self" subaggregation.
      // The details of the content of this aggregation are examined
      // in the "applies to aggregations with a paired filter" test.
      val firstAggregation = aggregations.head
      firstAggregation.subaggs should have length 2

      val firstNestedAggregation = firstAggregation.subaggs.head.asInstanceOf[NestedAggregation]

      firstNestedAggregation.subaggs.head.asInstanceOf[TermsAggregation] should have(
        aggregationField(s"${formatFieldName}.id")
      )

      //The second aggregation is Language, which has no corresponding
      // filter in this query, so does not have a "self" subaggregation
      val secondAggregation = aggregations(1)
      secondAggregation.subaggs should have length 1

      // But it should still be filtered using the format filter
      secondAggregation should have(
        filters(Seq(formatFilterQuery))
      )

      val secondNestedAggregation = secondAggregation.subaggs.head.asInstanceOf[NestedAggregation]

      secondNestedAggregation.subaggs.head.asInstanceOf[TermsAggregation] should have(
        aggregationField(s"${languagesFieldName}.id")
      )
    }

    it("applies all other aggregation-dependent filters to the paired filter") {
      val formatFilter = FormatFilter(Seq("bananas"))
      val languagesFilter = LanguagesFilter(Seq("en"))
      val genreFilter = GenreFilter(Seq("durian"))
      val filters = List(formatFilter, languagesFilter, genreFilter)

      val aggregations = WorksAggregationsBuilder.getAggregations(
        filters,
        List(
          WorkAggregationRequest.Format,
          WorkAggregationRequest.Languages,
          WorkAggregationRequest.GenreLabel
        )
      )

      aggregations should have length 3

      forAll(
        Table(
          ("agg", "matchingFilter"),
          // The aggregations and the filter list are expected to be
          // in the same order, so zipping them should result in
          // each aggregation being paired with its corresponding filter
          aggregations.zip(
            filters
          ): _*
        )
      ) { (agg: FilterAggregation, thisFilter: WorkFilter) =>
        val filterQuery = agg.query.asInstanceOf[BoolQuery]

        //Three filters are requested, each aggregation should
        // have only two.  i.e. not it's own
        filterQuery.filters should have length 2
        // And this ensures that it is the correct two.
        filterQuery.filters should contain theSameElementsAs filters
          .filterNot(_ == thisFilter).map(buildWorkFilterQuery)
      }
    }
  }
}
