package weco.api.search.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches._
import com.sksamuel.elastic4s.requests.searches.aggs._
import com.sksamuel.elastic4s.requests.searches.queries._
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.sort._
import weco.api.search.models._
import weco.api.search.models.request.{
  ProductionDateSortRequest,
  SortingOrder,
  WorkAggregationRequest
}
import weco.api.search.rest.PaginationQuery

object WorksRequestBuilder
    extends ElasticsearchRequestBuilder[WorkSearchOptions] {

  import ElasticsearchRequestBuilder._

  val idSort: FieldSort = fieldSort("query.id").order(SortOrder.ASC)

  def request(searchOptions: WorkSearchOptions, index: Index): SearchRequest = {
    implicit val s = searchOptions
    search(index)
      .aggs { filteredAggregationBuilder.filteredAggregations }
      .query { filteredQuery }
      .sortBy { sortBy }
      .limit { searchOptions.pageSize }
      .from { PaginationQuery.safeGetFrom(searchOptions) }
      .sourceInclude("display", "type")
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
    // Note: we want these aggregations to return every possible value, so we
    // want this to be as many formats as we support in the catalogue pipeline.
    //
    // At time of writing (May 2022), we have 23 different formats; I've used
    // 30 here so we have some headroom if we add new formats in future.
    case WorkAggregationRequest.Format =>
      TermsAggregation("format")
        .size(30)
        .field("aggregatableValues.workType")

    case WorkAggregationRequest.ProductionDate =>
      TermsAggregation("productionDates")
        .field("aggregatableValues.production.dates")
        .minDocCount(1)

    // We don't split genres into concepts, as the data isn't great,
    // and for rendering isn't useful at the moment.
    case WorkAggregationRequest.Genre =>
      TermsAggregation("genres")
        .size(20)
        .field("aggregatableValues.genres.label")

    case WorkAggregationRequest.Subject =>
      TermsAggregation("subjects")
        .size(20)
        .field("aggregatableValues.subjects.label")

    case WorkAggregationRequest.Contributor =>
      TermsAggregation("contributors")
        .size(20)
        .field("aggregatableValues.contributors.agent.label")

    case WorkAggregationRequest.Languages =>
      TermsAggregation("languages")
        .size(200)
        .field("aggregatableValues.languages")

    // Note: we want these aggregations to return every possible value, so we
    // want this to be as many licenses as we support in the catalogue pipeline.
    //
    // At time of writing (May 2022), we have 11 different licenses; I've used
    // 20 here so we have some headroom if we add new licenses in future.
    case WorkAggregationRequest.License =>
      TermsAggregation("license")
        .size(20)
        .field("aggregatableValues.items.locations.license")

    // Note: we want these aggregations to return every possible value, so we
    // want this to be as many availabilities as we support in the catalogue pipeline.
    //
    // At time of writing (May 2022), we have 3 different availabilities; I've used
    // 10 here so we have some headroom if we add new ones in future.
    case WorkAggregationRequest.Availabilities =>
      TermsAggregation("availabilities")
        .size(10)
        .field("aggregatableValues.availabilities")
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
        termsQuery(field = "data.workType", values = types)
      case DateRangeFilter(fromDate, toDate) =>
        val (gte, lte) =
          (fromDate map ElasticDate.apply, toDate map ElasticDate.apply)
        RangeQuery("data.production.dates.range.from", lte = lte, gte = gte)
      case LanguagesFilter(languageIds) =>
        termsQuery("query.languages.id", languageIds)
      case GenreFilter(genreQueries) =>
        termsQuery("query.genres.label.keyword", genreQueries)

      case SubjectLabelFilter(labels) =>
        termsQuery("query.subjects.label.keyword", labels)

      case ContributorsFilter(contributorQueries) =>
        termsQuery("query.contributors.agent.label.keyword", contributorQueries)

      case IdentifiersFilter(identifiers) =>
        termsQuery("query.identifiers.value", identifiers)

      case LicenseFilter(licenseIds) =>
        termsQuery("query.items.locations.license.id", licenseIds)
      case AccessStatusFilter(includes, excludes) =>
        includesExcludesQuery(
          field = "query.items.locations.accessConditions.status.id",
          includes = includes,
          excludes = excludes
        )
      case ItemsFilter(itemIds) =>
        should(
          termsQuery(
            field = "query.items.id",
            values = itemIds
          )
        )
      case ItemsIdentifiersFilter(itemSourceIdentifiers) =>
        should(
          termsQuery(
            field = "query.items.identifiers.value",
            values = itemSourceIdentifiers
          )
        )
      case ItemLocationTypeIdFilter(itemLocationTypeIds) =>
        termsQuery("query.items.locations.locationType.id", itemLocationTypeIds)

      case PartOfFilter(search_term) =>
        /*
         The partOf filter matches on either the id or the title of any ancestor.
         As it is vanishingly unlikely that an id_minter canonical id will coincidentally
         match the title of a Work or Series, this query can be a simple OR (i.e. bool.should),
         rather than have to introduce a new filter to the API to cover it.
         */
        bool(
          mustQueries = Nil,
          shouldQueries = Seq(
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
            matchPhraseQuery(
              field = "query.partOf.title",
              value = search_term
            ),
            termQuery(
              field = "query.partOf.id",
              value = search_term
            )
          ),
          notQueries = Nil
        )
      case PartOfTitleFilter(search_term) =>
        termQuery(
          field = "query.partOf.title.keyword",
          value = search_term
        )
      case AvailabilitiesFilter(availabilityIds) =>
        termsQuery(
          field = "query.availabilities.id",
          values = availabilityIds
        )
    }
}
