package weco.api.search.rest

import java.time.LocalDate
import akka.http.scaladsl.server.Directive
import io.circe.Decoder
import weco.api.search.models._
import weco.api.search.models.request.{
  ProductionDateSortRequest,
  SortRequest,
  SortingOrder,
  WorkAggregationRequest,
  WorkInclude,
  WorksIncludes
}
import weco.catalogue.display_model.models._
import weco.catalogue.internal_model.locations.AccessStatus
import weco.catalogue.internal_model.work.WorkType

case class SingleWorkParams(
  include: Option[WorksIncludes],
  _index: Option[String]
) extends QueryParams

object SingleWorkParams extends QueryParamsUtils {

  // This is a custom akka-http directive which extracts SingleWorkParams
  // data from the query string, returning an invalid response when any given
  // parameter is not correctly parsed. More info on custom directives is
  // available here:
  // https://doc.akka.io/docs/akka-http/current/routing-dsl/directives/custom-directives.html
  def parse: Directive[Tuple1[SingleWorkParams]] =
    parameters(
      "include".as[WorksIncludes].?,
      "_index".as[String].?
    ).tmap((SingleWorkParams.apply _).tupled(_))

  implicit val decodePaths: Decoder[List[String]] =
    decodeCommaSeparated

  implicit val includesDecoder: Decoder[WorksIncludes] =
    decodeOneOfCommaSeparated(
      "identifiers" -> WorkInclude.Identifiers,
      "items" -> WorkInclude.Items,
      "holdings" -> WorkInclude.Holdings,
      "subjects" -> WorkInclude.Subjects,
      "genres" -> WorkInclude.Genres,
      "contributors" -> WorkInclude.Contributors,
      "production" -> WorkInclude.Production,
      "languages" -> WorkInclude.Languages,
      "notes" -> WorkInclude.Notes,
      "images" -> WorkInclude.Images,
      "parts" -> WorkInclude.Parts,
      "partOf" -> WorkInclude.PartOf,
      "precededBy" -> WorkInclude.PrecededBy,
      "succeededBy" -> WorkInclude.SucceededBy
    ).emap(values => Right(WorksIncludes(values: _*)))
}

// We break up MultipleWorksParams into sub case-classes to avoid it getting too large --
// we're not sure of the exact limits on Scala case classes, but somewhere around
// 23 parameters weird stuff starts happening, e.g. values passed to the apply() method
// don't get reflected in the new case class.
case class ItemsParams(
  `items`: Option[ItemsFilter],
  `items.identifiers`: Option[ItemsIdentifiersFilter],
  `items.locations.license`: Option[LicenseFilter],
  `items.locations.locationType`: Option[ItemLocationTypeIdFilter],
  `items.locations.accessConditions.status`: Option[AccessStatusFilter]
)

case class PaginationParams(
  page: Option[Int],
  pageSize: Option[Int],
  sort: Option[List[SortRequest]],
  sortOrder: Option[SortingOrder]
)

case class WorkFilterParams(
  workType: Option[FormatFilter],
  `production.dates.from`: Option[LocalDate],
  `production.dates.to`: Option[LocalDate],
  languages: Option[LanguagesFilter],
  `genres.label`: Option[GenreFilter],
  `subjects.label`: Option[SubjectFilter],
  `contributors.agent.label`: Option[ContributorsFilter],
  identifiers: Option[IdentifiersFilter],
  partOf: Option[PartOfFilter],
  `partOf.title`: Option[PartOfTitleFilter],
  availabilities: Option[AvailabilitiesFilter],
  `type`: Option[WorkTypeFilter]
)

case class MultipleWorksParams(
  paginationParams: PaginationParams,
  itemsParams: ItemsParams,
  filterParams: WorkFilterParams,
  include: Option[WorksIncludes],
  aggregations: Option[List[WorkAggregationRequest]],
  query: Option[String],
  _queryType: Option[SearchQueryType],
  _index: Option[String]
) extends QueryParams
    with Paginated {

  lazy val page = paginationParams.page
  lazy val pageSize = paginationParams.pageSize

  def searchOptions(apiConfig: ApiConfig): WorkSearchOptions =
    WorkSearchOptions(
      searchQuery = query map { query =>
        SearchQuery(query, _queryType)
      },
      filters = filters,
      pageSize = pageSize.getOrElse(apiConfig.defaultPageSize),
      pageNumber = page.getOrElse(1),
      aggregations = aggregations.getOrElse(Nil),
      sortBy = paginationParams.sort.getOrElse(Nil),
      sortOrder = paginationParams.sortOrder.getOrElse(SortingOrder.Ascending)
    )

  private def filters: List[WorkFilter] =
    List(
      filterParams.workType,
      dateFilter,
      filterParams.languages,
      filterParams.`genres.label`,
      filterParams.`subjects.label`,
      filterParams.`contributors.agent.label`,
      filterParams.identifiers,
      itemsParams.`items`,
      itemsParams.`items.identifiers`,
      itemsParams.`items.locations.accessConditions.status`,
      itemsParams.`items.locations.license`,
      itemsParams.`items.locations.locationType`,
      filterParams.`type`,
      filterParams.partOf,
      filterParams.`partOf.title`,
      filterParams.availabilities
    ).flatten

  private def dateFilter: Option[DateRangeFilter] =
    (filterParams.`production.dates.from`, filterParams.`production.dates.to`) match {
      case (None, None)       => None
      case (dateFrom, dateTo) => Some(DateRangeFilter(dateFrom, dateTo))
    }
}

object MultipleWorksParams extends QueryParamsUtils {
  import SingleWorkParams.includesDecoder
  import CommonDecoders._

  // This is a custom akka-http directive which extracts MultipleWorksParams
  // data from the query string, returning an invalid response when any given
  // parameter is not correctly parsed. More info on custom directives is
  // available here:
  // https://doc.akka.io/docs/akka-http/current/routing-dsl/directives/custom-directives.html
  //
  // Scala has a max tuple size of about 22, so we break these up into nested
  // blocks to avoid hitting the limit.
  def parse: Directive[Tuple1[MultipleWorksParams]] =
    parameters(
      "items".as[ItemsFilter].?,
      "items.locations.license".as[LicenseFilter].?,
      "items.identifiers".as[ItemsIdentifiersFilter].?,
      "items.locations.locationType".as[ItemLocationTypeIdFilter].?,
      "items.locations.accessConditions.status".as[AccessStatusFilter].?,
      "page".as[Int].?,
      "pageSize".as[Int].?,
      "sort".as[List[SortRequest]].?,
      "sortOrder".as[SortingOrder].?,
      "_queryType".as[SearchQueryType].?,
      "_index".as[String].?,
      "query".as[String].?,
      "include".as[WorksIncludes].?,
      "aggregations".as[List[WorkAggregationRequest]].?
    ).tflatMap {
      case (
          items,
          license,
          identifiers,
          locationType,
          accessStatus,
          page,
          pageSize,
          sort,
          sortOrder,
          queryType,
          index,
          query,
          includes,
          aggregations
          ) =>
        val itemsParams = ItemsParams(
          items,
          identifiers,
          license,
          locationType,
          accessStatus
        )

        val paginationParams = PaginationParams(page, pageSize, sort, sortOrder)

        parameters(
          "workType".as[FormatFilter] ?,
          "production.dates.from".as[LocalDate].?,
          "production.dates.to".as[LocalDate].?,
          "languages".as[LanguagesFilter].?,
          "genres.label".as[GenreFilter].?,
          "subjects.label".as[SubjectFilter].?,
          "contributors.agent.label".as[ContributorsFilter].?,
          "identifiers".as[IdentifiersFilter].?,
          "partOf".as[PartOfFilter].?,
          "partOf.title".as[PartOfTitleFilter].?,
          "availabilities".as[AvailabilitiesFilter].?,
          "type".as[WorkTypeFilter].?
        ).tflatMap {
          case (
              format,
              dateFrom,
              dateTo,
              languages,
              genres,
              subjects,
              contributors,
              identifiers,
              partOf,
              partOfTitle,
              availabilities,
              workType
              ) =>
            val filterParams = WorkFilterParams(
              format,
              dateFrom,
              dateTo,
              languages,
              genres,
              subjects,
              contributors,
              identifiers,
              partOf,
              partOfTitle,
              availabilities,
              workType
            )

            val params = MultipleWorksParams(
              paginationParams = paginationParams,
              itemsParams = itemsParams,
              filterParams = filterParams,
              include = includes,
              aggregations = aggregations,
              query = query,
              _queryType = queryType,
              _index = index
            )
            validated(params.paginationErrors, params)
        }
    }

  implicit val formatFilter: Decoder[FormatFilter] =
    stringListFilter(FormatFilter)

  implicit val workTypeFilter: Decoder[WorkTypeFilter] =
    decodeOneOfCommaSeparated(
      "Collection" -> WorkType.Collection,
      "Series" -> WorkType.Series,
      "Section" -> WorkType.Section
    ).emap(values => Right(WorkTypeFilter(values)))

  implicit val itemLocationTypeIdFilter: Decoder[ItemLocationTypeIdFilter] =
    stringListFilter(ItemLocationTypeIdFilter)

  implicit val languagesFilter: Decoder[LanguagesFilter] =
    stringListFilter(LanguagesFilter)

  implicit val subjectFilter: Decoder[SubjectFilter] =
    stringListFilter(SubjectFilter)

  implicit val identifiersFilter: Decoder[IdentifiersFilter] =
    stringListFilter(IdentifiersFilter)

  implicit val itemsFilter: Decoder[ItemsFilter] =
    stringListFilter(ItemsFilter)

  implicit val itemsIdentifiersFilter: Decoder[ItemsIdentifiersFilter] =
    stringListFilter(ItemsIdentifiersFilter)

  implicit val partOf: Decoder[PartOfFilter] =
    Decoder.decodeString.map(PartOfFilter)

  implicit val partOfTitle: Decoder[PartOfTitleFilter] =
    Decoder.decodeString.map(PartOfTitleFilter)

  implicit val availabilitiesFilter: Decoder[AvailabilitiesFilter] =
    stringListFilter(AvailabilitiesFilter)

  implicit val accessStatusFilter: Decoder[AccessStatusFilter] =
    decodeIncludesAndExcludes(
      "open" -> AccessStatus.Open,
      "open-with-advisory" -> AccessStatus.OpenWithAdvisory,
      "restricted" -> AccessStatus.Restricted,
      "closed" -> AccessStatus.Closed,
      "licensed-resources" -> AccessStatus.LicensedResources(),
      "unavailable" -> AccessStatus.Unavailable,
      "permission-required" -> AccessStatus.PermissionRequired
    ).emap {
      case (includes, excludes) => Right(AccessStatusFilter(includes, excludes))
    }

  implicit val aggregationsDecoder: Decoder[List[WorkAggregationRequest]] =
    decodeOneOfCommaSeparated(
      "workType" -> WorkAggregationRequest.Format,
      "genres.label" -> WorkAggregationRequest.Genre,
      "production.dates" -> WorkAggregationRequest.ProductionDate,
      "subjects.label" -> WorkAggregationRequest.Subject,
      "languages" -> WorkAggregationRequest.Languages,
      "contributors.agent.label" -> WorkAggregationRequest.Contributor,
      "items.locations.license" -> WorkAggregationRequest.License,
      "availabilities" -> WorkAggregationRequest.Availabilities
    )

  implicit val sortDecoder: Decoder[List[SortRequest]] =
    decodeOneOfCommaSeparated(
      "production.dates" -> ProductionDateSortRequest
    )

  implicit val sortOrderDecoder: Decoder[SortingOrder] =
    decodeOneOf(
      "asc" -> SortingOrder.Ascending,
      "desc" -> SortingOrder.Descending
    )

  implicit val _queryTypeDecoder: Decoder[SearchQueryType] =
    decodeOneWithDefaultOf(
      SearchQueryType.default,
      "MultiMatcher" -> SearchQueryType.MultiMatcher
    )
}
