package weco.api.search.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches._
import com.sksamuel.elastic4s.requests.searches.aggs._
import com.sksamuel.elastic4s.requests.searches.queries._
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.sort._
import weco.api.search.models._
import weco.api.search.rest.PaginationQuery
import weco.catalogue.display_model.models.{
  ProductionDateSortRequest,
  SortingOrder,
  WorkAggregationRequest
}
import weco.catalogue.internal_model.locations.License
import weco.catalogue.internal_model.work._

object WorksRequestBuilder
    extends ElasticsearchRequestBuilder[WorkSearchOptions] {

  import ElasticsearchRequestBuilder._

  val idSort: FieldSort = fieldSort("state.canonicalId").order(SortOrder.ASC)

  def request(searchOptions: WorkSearchOptions, index: Index): SearchRequest = {
    implicit val s = searchOptions
    search(index)
      .aggs { filteredAggregationBuilder.filteredAggregations }
      .query { filteredQuery }
      .sortBy { sortBy }
      .limit { searchOptions.pageSize }
      .from { PaginationQuery.safeGetFrom(searchOptions) }
  }

  private def filteredAggregationBuilder(
    implicit searchOptions: WorkSearchOptions
  ) =
    new WorkFiltersAndAggregationsBuilder(
      aggregationRequests = searchOptions.aggregations,
      filters = searchOptions.filters,
      requestToAggregation = toAggregation,
      filterToQuery = buildWorkFilterQuery,
      searchQuery = searchQuery
    )

  private def toAggregation(aggReq: WorkAggregationRequest) = aggReq match {
    case WorkAggregationRequest.Format =>
      TermsAggregation("format")
        .size(Format.values.size)
        .field("data.format.id")

    case WorkAggregationRequest.ProductionDate =>
      DateHistogramAggregation("productionDates")
        .calendarInterval(DateHistogramInterval.Year)
        .field("data.production.dates.range.from")
        .minDocCount(1)

    // We don't split genres into concepts, as the data isn't great,
    // and for rendering isn't useful at the moment.
    case WorkAggregationRequest.Genre =>
      TermsAggregation("genres")
        .size(20)
        .field("data.genres.concepts.label.keyword")

    case WorkAggregationRequest.Subject =>
      TermsAggregation("subjects")
        .size(20)
        .field("data.subjects.label.keyword")

    case WorkAggregationRequest.Contributor =>
      TermsAggregation("contributors")
        .size(20)
        .field("state.derivedData.contributorAgents")

    case WorkAggregationRequest.Languages =>
      TermsAggregation("languages")
        .size(200)
        .field("data.languages.id")

    case WorkAggregationRequest.License =>
      TermsAggregation("license")
        .size(License.values.size)
        .field("data.items.locations.license.id")

    case WorkAggregationRequest.Availabilities =>
      TermsAggregation("availabilities")
        .size(Availability.values.size)
        .field("state.availabilities.id")
  }

  private def sortBy(implicit searchOptions: WorkSearchOptions) =
    if (searchOptions.searchQuery.isDefined || searchOptions.mustQueries.nonEmpty) {
      sort :+ scoreSort(SortOrder.DESC) :+ idSort
    } else {
      sort :+ idSort
    }

  private def sort(implicit searchOptions: WorkSearchOptions) =
    searchOptions.sortBy
      .map {
        case ProductionDateSortRequest => "data.production.dates.range.from"
      }
      .map { FieldSort(_).order(sortOrder) }

  private def sortOrder(implicit searchOptions: WorkSearchOptions) =
    searchOptions.sortOrder match {
      case SortingOrder.Ascending  => SortOrder.ASC
      case SortingOrder.Descending => SortOrder.DESC
    }

  private def searchQuery(
    implicit searchOptions: WorkSearchOptions
  ): BoolQuery =
    searchOptions.searchQuery
      .map {
        case SearchQuery(query, queryType) =>
          queryType.toEsQuery(query)
      }
      .getOrElse { boolQuery }

  private def filteredQuery(
    implicit searchOptions: WorkSearchOptions
  ): BoolQuery =
    searchQuery
      .filter {
        (VisibleWorkFilter :: searchOptions.filters)
          .map(buildWorkFilterQuery)
      }

  private def buildWorkFilterQuery(workFilter: WorkFilter): Query =
    workFilter match {
      case VisibleWorkFilter =>
        termQuery(field = "type", value = "Visible")
      case FormatFilter(formatIds) =>
        termsQuery(field = "data.format.id", values = formatIds)
      case WorkTypeFilter(types) =>
        termsQuery(
          field = "data.workType",
          values = types.map(WorkType.getName)
        )
      case DateRangeFilter(fromDate, toDate) =>
        val (gte, lte) =
          (fromDate map ElasticDate.apply, toDate map ElasticDate.apply)
        RangeQuery("data.production.dates.range.from", lte = lte, gte = gte)
      case LanguagesFilter(languageIds) =>
        termsQuery(field = "data.languages.id", values = languageIds)
      case GenreFilter(genreQueries) =>
        termsQuery("data.genres.label.keyword", genreQueries)
      case SubjectFilter(subjectQueries) =>
        termsQuery("data.subjects.label.keyword", subjectQueries)
      case ContributorsFilter(contributorQueries) =>
        termsQuery("data.contributors.agent.label.keyword", contributorQueries)
      case LicenseFilter(licenseIds) =>
        termsQuery(
          field = "data.items.locations.license.id",
          values = licenseIds
        )
      case IdentifiersFilter(identifiers) =>
        should(
          termsQuery(
            field = "state.sourceIdentifier.value",
            values = identifiers
          ),
          termsQuery(
            field = "data.otherIdentifiers.value",
            values = identifiers
          )
        )
      case AccessStatusFilter(includes, excludes) =>
        includesExcludesQuery(
          field = "data.items.locations.accessConditions.status.type",
          includes = includes.map(_.name),
          excludes = excludes.map(_.name)
        )
      case ItemLocationTypeIdFilter(itemLocationTypeIds) =>
        termsQuery(
          field = "data.items.locations.locationType.id",
          values = itemLocationTypeIds
        )
      case PartOfFilter(search_term) =>
        /*
         The partOf filter matches on either the id or the title of any ancestor.
         As it is vanishingly unlikely that an id_minter canonical id will coincidentally
         match the title of a Work or Series, this query can be a simple OR (i.e. bool.should),
         rather than have to introduce a new filter to the API to cover it.
         */
        bool(
          Seq.empty,
          Seq(
            /*
             title is an analysed field, so cannot be matched by a term query.
             Instead, a matchPhrase is used. This will match a contiguous substring within
             the title as well as the whole title
             (e.g. search for Wellcome Malay and you will get Wellcome Malay 7 and Wellcome Malay 8).
             In most situations, this is likely to be the desired result anyway.  However for Series
             linking, this may provide some incorrect results.
             e.g. 'The Journal of Philosophy' is the title of a philosophy journal, but so is
             'The journal of philosophy, psychology and scientific methods'.
             If this does cause unwanted results in "real life", then we can consider storing a
             separate non-analysed version of title for term matching.
             */
            matchPhraseQuery(field = "state.relations.ancestors.title", value = search_term),
            termQuery(field = "state.relations.ancestors.id", value = search_term)
          ),
          Seq.empty
        )

      case AvailabilitiesFilter(availabilityIds) =>
        termsQuery(
          field = "state.availabilities.id",
          values = availabilityIds
        )
    }
}
