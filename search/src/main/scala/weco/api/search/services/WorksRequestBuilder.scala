package weco.api.search.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches.aggs._
import com.sksamuel.elastic4s.requests.searches.queries._
import com.sksamuel.elastic4s.requests.searches.sort._

import weco.api.search.models._
import weco.api.search.models.request.{
  ProductionDateSortRequest,
  SortingOrder,
  WorkAggregationRequest
}
import weco.api.search.rest.PaginationQuery
import weco.api.search.elasticsearch.templateSearch.TemplateSearchRequest

object WorksRequestBuilder
    extends ElasticsearchRequestBuilder[WorkSearchOptions]
    with WorksTemplateSearchBuilder
    with Encoders {

  import ElasticsearchRequestBuilder._

  val idSort: FieldSort = fieldSort("query.id").order(SortOrder.ASC)

  def request(
    searchOptions: WorkSearchOptions,
    index: Index
  ): Right[Nothing, TemplateSearchRequest] = {
    implicit val s: WorkSearchOptions = searchOptions
    val splitFilterRequests = searchOptions.filters.groupBy {
      case _: Pairable => Pairable
      case _           => Unpairable
    }

    val unpairables = splitFilterRequests.getOrElse(Unpairable, Nil)
    val pairables = splitFilterRequests
      .getOrElse(Pairable, Nil)
      .asInstanceOf[List[WorkFilter with Pairable]]

    Right(
      searchRequest(
        indexes = Seq(index.name),
        params = SearchTemplateParams(
          query = searchOptions.searchQuery.map(_.query),
          from = PaginationQuery.safeGetFrom(searchOptions),
          size = searchOptions.pageSize,
          sortByDate = dateOrder,
          sortByScore = searchOptions.searchQuery.isDefined,
          includes = Seq("display", "type"),
          aggs = filteredAggregationBuilder(pairables).filteredAggregations,
          preFilter =
            (VisibleWorkFilter :: unpairables).collect(buildWorkFilterQuery),
          postFilter = Some(
            must(
              pairables.collect(buildWorkFilterQuery)
            )
          )
        )
      )
    )
  }

  private def filteredAggregationBuilder(
    filters: List[WorkFilter with Pairable]
  )(implicit searchOptions: WorkSearchOptions) =
    new WorkFiltersAndAggregationsBuilder(
      aggregationRequests = searchOptions.aggregations,
      filters = filters,
      requestToAggregation = toAggregation,
      filterToQuery = buildWorkFilterQuery
    )

  /**
   * Each aggregatable field is indexed as a nested field with an `id` value and a `label` value. All aggregations
   * are `id`-based, with a `label`-based sub-aggregation to get Elasticsearch to return all `label` values associated
   * with each `id` bucket. (Usually each `id` value only has one one `label` value associated with it, but not always.
   * For example, different Works can use different labels for a given LoC Subject Heading.)
   */
  private def toIdBasedAggregation(
    aggregationName: String,
    nestedFieldPath: String,
    size: Int
  ) =
    TermsAggregation(aggregationName)
      .size(size)
      .field(s"$nestedFieldPath.id")
      .subAggregations(termsAgg("labels", s"$nestedFieldPath.label").size(1))

  @deprecated("This method is included for backward compatibility reasons and will be removed soon.", "25-10-2024")
  private def toLegacyAggregation(
    aggregationName: String,
    aggregatedFieldPath: String,
    size: Int
  ) =
    TermsAggregation(aggregationName).size(size).field(aggregatedFieldPath)

  private def toAggregation(aggReq: WorkAggregationRequest) = aggReq match {
    // Note: we want these aggregations to return every possible value, so we
    // want this to be as many formats as we support in the catalogue pipeline.
    //
    // At time of writing (May 2022), we have 23 different formats; I've used
    // 30 here so we have some headroom if we add new formats in future.
    case WorkAggregationRequest.Format =>
      toIdBasedAggregation("format", "aggregatableValues.workType", size = 30)

    case WorkAggregationRequest.ProductionDate =>
      toLegacyAggregation(
        "productionDates",
        "aggregatableValues.production.dates.label",
        size = 10
      ).minDocCount(1)

    case WorkAggregationRequest.Genre =>
      toIdBasedAggregation("genres", "aggregatableValues.genres", size = 20)

    case WorkAggregationRequest.Subject =>
      toIdBasedAggregation("subjects", "aggregatableValues.subjects", size = 20)

    case WorkAggregationRequest.Contributor =>
      toIdBasedAggregation(
        "contributors",
        "aggregatableValues.contributors.agent",
        size = 20
      )

    case WorkAggregationRequest.Languages =>
      toIdBasedAggregation(
        "languages",
        "aggregatableValues.languages",
        size = 200)

    // Note: we want these aggregations to return every possible value, so we
    // want this to be as many licenses as we support in the catalogue pipeline.
    //
    // At time of writing (May 2022), we have 11 different licenses; I've used
    // 20 here so we have some headroom if we add new licenses in future.
    case WorkAggregationRequest.License =>
      toIdBasedAggregation(
        "license",
        "aggregatableValues.items.locations.license",
        size = 20
      )

    // Note: we want these aggregations to return every possible value, so we
    // want this to be as many availabilities as we support in the catalogue pipeline.
    //
    // At time of writing (May 2022), we have 3 different availabilities; I've used
    // 10 here so we have some headroom if we add new ones in future.
    case WorkAggregationRequest.Availabilities =>
      toIdBasedAggregation(
        "availabilities",
        "aggregatableValues.availabilities",
        size = 10
      )
  }

  private def dateOrder(
    implicit
    searchOptions: WorkSearchOptions): Option[SortingOrder] =
    searchOptions.sortBy collectFirst {
      case ProductionDateSortRequest =>
        searchOptions.sortOrder
    }

  val buildWorkFilterQuery: PartialFunction[WorkFilter, Query] = {
    case VisibleWorkFilter =>
      termQuery(field = "type", value = "Visible")
    case FormatFilter(formatIds) =>
      termsQuery(field = "filterableValues.format.id", values = formatIds)
    case WorkTypeFilter(types) =>
      termsQuery(field = "filterableValues.workType", values = types)
    case DateRangeFilter(fromDate, toDate) =>
      val (gte, lte) =
        (fromDate map ElasticDate.apply, toDate map ElasticDate.apply)
      RangeQuery(
        "filterableValues.production.dates.range.from",
        lte = lte,
        gte = gte
      )
    case LanguagesFilter(languageIds) =>
      termsQuery(
        "filterableValues.languages.id",
        languageIds
      )
    case GenreFilter(genreQueries) =>
      termsQuery(
        "filterableValues.genres.label",
        genreQueries
      )
    case GenreConceptFilter(conceptIds) if conceptIds.nonEmpty =>
      termsQuery(
        "filterableValues.genres.concepts.id",
        conceptIds
      )
    case SubjectLabelFilter(labels) =>
      termsQuery(
        "filterableValues.subjects.label",
        labels
      )
    case SubjectConceptFilter(conceptIds) if conceptIds.nonEmpty =>
      termsQuery(
        "filterableValues.subjects.concepts.id",
        conceptIds
      )
    case ContributorsFilter(contributorQueries) =>
      termsQuery(
        "filterableValues.contributors.agent.label",
        contributorQueries
      )

    case ContributorsConceptFilter(conceptIds) =>
      termsQuery(
        "filterableValues.contributors.agent.id",
        conceptIds
      )

    case IdentifiersFilter(identifiers) =>
      // TODO we're using the value in `query` because it's lowercase-normalised,
      // whereas the value in `filterableValues` is just a plain keyword and so
      // is case sensitive. Particularly for archive refnos, case-insensitivity
      // seems to be appropriate for this filter. We should add the normalizer
      // to `filterableValues` and update the field used here once that's reindexed.
      termsQuery("query.identifiers.value", identifiers)

    case LicenseFilter(licenseIds) =>
      termsQuery("filterableValues.items.locations.license.id", licenseIds)
    case AccessStatusFilter(includes, excludes) =>
      includesExcludesQuery(
        field = "filterableValues.items.locations.accessConditions.status.id",
        includes = includes,
        excludes = excludes
      )
    case ItemsFilter(itemIds) =>
      should(
        termsQuery(
          field = "filterableValues.items.id",
          values = itemIds
        )
      )
    case ItemsIdentifiersFilter(itemSourceIdentifiers) =>
      should(
        termsQuery(
          field = "filterableValues.items.identifiers.value",
          values = itemSourceIdentifiers
        )
      )
    case ItemLocationTypeIdFilter(itemLocationTypeIds) =>
      termsQuery(
        "filterableValues.items.locations.locationType.id",
        itemLocationTypeIds
      )

    case PartOfFilter(search_term) =>
      termQuery(
        field = "filterableValues.partOf.id",
        value = search_term
      )
    case PartOfTitleFilter(search_term) =>
      termQuery(
        field = "filterableValues.partOf.title",
        value = search_term
      )
    case AvailabilitiesFilter(availabilityIds) =>
      termsQuery(
        field = "filterableValues.availabilities.id",
        values = availabilityIds
      )
  }

}
