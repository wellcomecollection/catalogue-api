package weco.api.search.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches.queries._
import com.sksamuel.elastic4s.requests.searches.sort._
import weco.api.search.models._
import weco.api.search.models.request.{ProductionDateSortRequest, SortingOrder}
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
          aggs = WorksAggregationsBuilder.getAggregations(pairables, searchOptions.aggregations),
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

  private def dateOrder(implicit
    searchOptions: WorkSearchOptions
  ): Option[SortingOrder] =
    searchOptions.sortBy collectFirst { case ProductionDateSortRequest =>
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
