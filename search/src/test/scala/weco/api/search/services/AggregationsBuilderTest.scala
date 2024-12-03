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
import weco.api.search.models.{FormatFilter, GenreLabelFilter, LanguagesFilter, WorkFilter}
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

  case class DecomposedAggregation(
    filtered: FilterAggregation,
    filteredNested: NestedAggregation,
    filteredNestedSelf: Option[NestedAggregation],
    unfilteredNestedSelf: Option[NestedAggregation],
    filteredTerms: TermsAggregation,
    filteredSelfTerms: Option[TermsAggregation],
    unfilteredSelfTerms: Option[TermsAggregation]
  )

  // See AggregationsBuilder class for an explanation of why aggregations are structured this way
  private def decomposeRootAggregation(rootAggregation: FilterAggregation): DecomposedAggregation = {
    val filteredAggregation = rootAggregation.subaggs.head.asInstanceOf[FilterAggregation]
    val filteredNestedAggregation = filteredAggregation.subaggs.head.asInstanceOf[NestedAggregation]
    val filteredNestedSelfAggregation = filteredAggregation.subaggs.lift(1).map(_.asInstanceOf[NestedAggregation])
    val unfilteredNestedSelfAggregation = rootAggregation.subaggs.lift(1).map(_.asInstanceOf[NestedAggregation])

    val filteredTermsAggregation = filteredNestedAggregation.subaggs.head.asInstanceOf[TermsAggregation]
    val filteredTermsSelfAggregation = filteredNestedSelfAggregation.map(_.subaggs.head.asInstanceOf[TermsAggregation])
    val unfilteredTermsSelfAggregation = unfilteredNestedSelfAggregation.map(_.subaggs.head.asInstanceOf[TermsAggregation])

    DecomposedAggregation(
      filtered=filteredAggregation,
      filteredNested=filteredNestedAggregation,
      filteredNestedSelf=filteredNestedSelfAggregation,
      unfilteredNestedSelf = unfilteredNestedSelfAggregation,
      filteredTerms = filteredTermsAggregation,
      filteredSelfTerms = filteredTermsSelfAggregation,
      unfilteredSelfTerms = unfilteredTermsSelfAggregation
    )
  }


  describe("aggregation-level filtering") {
    it("applies to aggregations with a paired filter") {
      val formatFilter = FormatFilter(Seq("bananas"))
      val languagesFilter = LanguagesFilter(Seq("en"))

      val aggregations = WorksAggregationsBuilder.getAggregations(
        List(formatFilter, languagesFilter),
        List(WorkAggregationRequest.Format, WorkAggregationRequest.Languages)
      )

      aggregations should have length 2

      val firstAggregation = decomposeRootAggregation(aggregations.head)

      // The first aggregation is the Format one, filtered with the languages filter.
      firstAggregation.filtered should have(
        filters(Seq(languagesFilterQuery)),
      )
      firstAggregation.filteredTerms should have(
        aggregationField(s"${formatFieldName}.id")
      )

      inside(firstAggregation.filteredSelfTerms.get) {
        case selfFilter: TermsAggregation =>
          selfFilter should have(
            aggregationField(s"${formatFieldName}.id")
          )
      }

      val secondAggregation = decomposeRootAggregation(aggregations(1))

      // The second aggregation is the Languages one, filtered with the format filter.
      secondAggregation.filtered should have(
        filters(Seq(formatFilterQuery)),
      )
      secondAggregation.filteredTerms should have(
        aggregationField(s"${languagesFieldName}.id")
      )

      inside(secondAggregation.filteredSelfTerms.get) { case selfFilter: TermsAggregation =>
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
      val filterAggregation = decomposeRootAggregation(aggregations.loneElement)

      // This marks the absence of the "self" aggregation
      filterAggregation.filteredNestedSelf shouldBe None
      filterAggregation.unfilteredNestedSelf shouldBe None

      filterAggregation.filteredTerms.field.get shouldBe s"${formatFieldName}.id"
    }

    it("includes a label-based subaggregation") {
      val languagesFilter = LanguagesFilter(Seq("en"))

      val aggregations = WorksAggregationsBuilder.getAggregations(
        List(languagesFilter),
        List(WorkAggregationRequest.Format)
      )

      // The aggregation list is just the requested aggregation
      // filtered by the requested (unpaired) filter.
      val filterAggregation = decomposeRootAggregation(aggregations.loneElement)

      filterAggregation.filteredNestedSelf shouldBe None
      filterAggregation.unfilteredNestedSelf shouldBe None

      val labelSubaggregation = filterAggregation.filteredTerms.subaggs.loneElement.asInstanceOf[TermsAggregation]

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
      val firstAggregation = decomposeRootAggregation(aggregations.head)
      firstAggregation.filtered.subaggs should have length 2

      firstAggregation.filteredTerms should have(
        aggregationField(s"${formatFieldName}.id")
      )

      //The second aggregation is Language, which has no corresponding
      // filter in this query, so does not have a "self" subaggregation
      val secondAggregation = decomposeRootAggregation(aggregations(1))
      secondAggregation.filtered.subaggs should have length 1

      // But it should still be filtered using the format filter
      secondAggregation.filtered should have(
        filters(Seq(formatFilterQuery))
      )

      secondAggregation.filteredTerms should have(
        aggregationField(s"${languagesFieldName}.id")
      )
    }

    it("applies all other aggregation-dependent filters to the paired filter") {
      val formatFilter = FormatFilter(Seq("bananas"))
      val languagesFilter = LanguagesFilter(Seq("en"))
      val genreFilter = GenreLabelFilter(Seq("durian"))
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
        val filteredAggregation = decomposeRootAggregation(agg).filtered
        val filterQuery = filteredAggregation.query.asInstanceOf[BoolQuery]

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
