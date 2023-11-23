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
          preFilter = buildWorkFilterQuery(VisibleWorkFilter :: unpairables),
          postFilter = Some(
            must(
              buildWorkFilterQuery(pairables)
            )
          )
        )
      )
    )
  }

  private def filteredAggregationBuilder(
    filters: List[WorkFilter with Pairable]
  )(
    implicit searchOptions: WorkSearchOptions
  ) =
    new WorkFiltersAndAggregationsBuilder(
      aggregationRequests = searchOptions.aggregations,
      filters = filters,
      requestToAggregation = toAggregation,
      filterToQuery = buildWorkFilterQuery
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

  private def dateOrder(
    implicit searchOptions: WorkSearchOptions
  ): Option[SortingOrder] =
    searchOptions.sortBy collectFirst {
      case ProductionDateSortRequest =>
        searchOptions.sortOrder
    }

  private def buildWorkFilterQuery(filters: Seq[WorkFilter]): Seq[Query] =
    filters.map {
      buildWorkFilterQuery
    } filter (_ != NoopQuery)

  private def buildWorkFilterQuery(workFilter: WorkFilter): Query =
    workFilter match {
      case VisibleWorkFilter =>
        termQuery(field = "type", value = "Visible")
      case FormatFilter(formatIds) =>
        termsQuery(field = "filterableValues.format.id", values = formatIds)
      case WorkTypeFilter(types) =>
        termsQuery(field = "filterableValues.workType", values = types)
      case DateRangeFilter(fromDate, toDate) =>
        val (gte, lte) =
          (fromDate map ElasticDate.apply, toDate map ElasticDate.apply)
        RangeQuery("filterableValues.production.dates.range.from", lte = lte, gte = gte)
      case LanguagesFilter(languageIds) =>
        termsQuery("filterableValues.languages.id", languageIds)
      case GenreFilter(genreQueries) =>
        termsQuery("filterableValues.genres.label.keyword", genreQueries)
      case GenreConceptFilter(conceptIds) =>
        if (conceptIds.isEmpty) NoopQuery
        else termsQuery("filterableValues.genres.concepts.id", conceptIds)

      case SubjectLabelFilter(labels) =>
        termsQuery("filterableValues.subjects.label.keyword", labels)

      case ContributorsFilter(contributorQueries) =>
        termsQuery("filterableValues.contributors.agent.label.keyword", contributorQueries)

      case IdentifiersFilter(identifiers) =>
        termsQuery("filterableValues.identifiers.value", identifiers)

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
        termsQuery("filterableValues.items.locations.locationType.id", itemLocationTypeIds)

      case PartOfFilter(search_term) =>
        termQuery(
          field = "filterableValues.partOf.id",
          value = search_term
        )
      case PartOfTitleFilter(search_term) =>
        termQuery(
          field = "filterableValues.partOf.title.keyword",
          value = search_term
        )
      case AvailabilitiesFilter(availabilityIds) =>
        termsQuery(
          field = "filterableValues.availabilities.id",
          values = availabilityIds
        )
    }

}
