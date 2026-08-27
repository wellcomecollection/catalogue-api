package weco.api.search.rest

import org.apache.pekko.http.scaladsl.server.Directive
import io.circe.Decoder
import weco.api.search.models._
import weco.api.search.models.request._
import weco.catalogue.display_model.locations.{
  CatalogueAccessMethod,
  CatalogueAccessStatus
}

import java.time.LocalDate

case class SingleWorkParams(
  include: Option[WorksIncludes]
) extends QueryParams

object SingleWorkParams extends QueryParamsUtils {

  // This is a custom pekko-http directive which extracts SingleWorkParams
  // data from the query string, returning an invalid response when any given
  // parameter is not correctly parsed. More info on custom directives is
  // available here:
  // https://pekko.apache.org/docs/pekko-http/1.0/routing-dsl/directives/custom-directives.html
  def parse: Directive[Tuple1[SingleWorkParams]] =
    parameters(
      "include".as[WorksIncludes].?
    ).tmap {
      case Tuple1(include) =>
        SingleWorkParams(include)
    }

  implicit val decodePaths: Decoder[List[String]] =
    decodeCommaSeparated

  // Exposed so that OpenApiSpecEnumTest can assert the spec's `include` enum
  // still matches what this decoder accepts.
  val includeValues: Seq[(String, WorkInclude)] = Seq(
    "identifiers" -> WorkInclude.Identifiers,
    "items" -> WorkInclude.Items,
    "holdings" -> WorkInclude.Holdings,
    "subjects" -> WorkInclude.Subjects,
    "genres" -> WorkInclude.Genres,
    "contributors" -> WorkInclude.Contributors,
    "production" -> WorkInclude.Production,
    "languages" -> WorkInclude.Languages,
    "archive" -> WorkInclude.Archive,
    "collection" -> WorkInclude.Collection,
    "notes" -> WorkInclude.Notes,
    "formerFrequency" -> WorkInclude.FormerFrequency,
    "designation" -> WorkInclude.Designation,
    "images" -> WorkInclude.Images,
    "parts" -> WorkInclude.Parts,
    "partOf" -> WorkInclude.PartOf
  )

  /** Accepted and ignored, deliberately undocumented. The pipeline stopped
    * emitting these fields in the move to catalogue_graph, but iiif-builder
    * still sends them on every request, so a hard 400 would break IIIF
    * manifest building. Remove once iiif-builder stops sending them.
    */
  val deprecatedIncludeValues: Seq[String] = Seq("precededBy", "succeededBy")

  implicit val includesDecoder: Decoder[WorksIncludes] =
    decodeCommaSeparated
      .map(_.filterNot(deprecatedIncludeValues.contains))
      .emap { strs =>
        mapStringsToValues(strs, includeValues.toMap).left.map { invalidStrs =>
          invalidValuesMsg(invalidStrs, includeValues.map(_._1).toList)
        }
      }
      .emap(values => Right(WorksIncludes(values: _*)))
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
  `items.locations.accessConditions.status`: Option[AccessStatusFilter],
  `items.locations.accessConditions.method`: Option[AccessMethodFilter],
  `items.locations.createdDate.from`: Option[LocalDate],
  `items.locations.createdDate.to`: Option[LocalDate]
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
  `archive.category`: Option[ArchiveCategoryFilter],
  `genres.label`: Option[GenreLabelFilter],
  `genres`: Option[GenreIdFilter],
  `subjects.label`: Option[SubjectLabelFilter],
  `subjects`: Option[SubjectIdFilter],
  `contributors.agent.label`: Option[ContributorsLabelFilter],
  `contributors.agent`: Option[ContributorsIdFilter],
  identifiers: Option[IdentifiersFilter],
  partOf: Option[PartOfFilter],
  `partOf.title`: Option[PartOfTitleFilter],
  availabilities: Option[AvailabilitiesFilter],
  `type`: Option[WorkTypeFilter],
  `collection.isRoot`: Option[CollectionIsRootFilter],
  `collection.root`: Option[CollectionRootFilter]
)

case class MultipleWorksParams(
  paginationParams: PaginationParams,
  itemsParams: ItemsParams,
  filterParams: WorkFilterParams,
  include: Option[WorksIncludes],
  aggregations: Option[List[WorkAggregationRequest]],
  query: Option[String]
) extends QueryParams
    with Paginated {

  lazy val page = paginationParams.page
  lazy val pageSize = paginationParams.pageSize

  def searchOptions(
    apiConfig: ApiConfig,
    semanticConfig: Option[SemanticConfig]
  ): WorkSearchOptions =
    WorkSearchOptions(
      searchQuery = query map { query =>
        SearchQuery(query)
      },
      filters = filters,
      pageSize = pageSize.getOrElse(apiConfig.defaultPageSize),
      pageNumber = page.getOrElse(1),
      aggregations = aggregations.getOrElse(Nil),
      sortBy = paginationParams.sort.getOrElse(Nil),
      sortOrder = paginationParams.sortOrder.getOrElse(SortingOrder.Ascending),
      semanticConfig = semanticConfig
    )

  private def filters: List[WorkFilter] =
    List(
      filterParams.workType,
      dateFilter,
      createdDateFilter,
      filterParams.languages,
      filterParams.`archive.category`,
      filterParams.`genres.label`,
      filterParams.`genres`,
      filterParams.`subjects.label`,
      filterParams.`subjects`,
      filterParams.`contributors.agent.label`,
      filterParams.`contributors.agent`,
      filterParams.identifiers,
      itemsParams.`items`,
      itemsParams.`items.identifiers`,
      itemsParams.`items.locations.accessConditions.status`,
      itemsParams.`items.locations.accessConditions.method`,
      itemsParams.`items.locations.license`,
      itemsParams.`items.locations.locationType`,
      filterParams.`type`,
      filterParams.partOf,
      filterParams.`partOf.title`,
      filterParams.availabilities,
      filterParams.`collection.isRoot`,
      filterParams.`collection.root`
    ).flatten

  private def dateFilter: Option[DateRangeFilter] =
    (
      filterParams.`production.dates.from`,
      filterParams.`production.dates.to`
    ) match {
      case (None, None)       => None
      case (dateFrom, dateTo) => Some(DateRangeFilter(dateFrom, dateTo))
    }

  private def createdDateFilter: Option[ItemsLocationsCreatedDateFilter] =
    (
      itemsParams.`items.locations.createdDate.from`,
      itemsParams.`items.locations.createdDate.to`
    ) match {
      case (None, None) => None
      case (dateFrom, dateTo) =>
        Some(ItemsLocationsCreatedDateFilter(dateFrom, dateTo))
    }
}

object MultipleWorksParams extends QueryParamsUtils {
  import CommonDecoders._
  import SingleWorkParams.includesDecoder

  // This is a custom pekko-http directive which extracts MultipleWorksParams
  // data from the query string, returning an invalid response when any given
  // parameter is not correctly parsed. More info on custom directives is
  // available here:
  // https://pekko.apache.org/docs/pekko-http/1.0/routing-dsl/directives/custom-directives.html
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
      "items.locations.accessConditions.method".as[AccessMethodFilter].?,
      "items.locations.createdDate.from".as[LocalDate].?,
      "items.locations.createdDate.to".as[LocalDate].?,
      "page".as[Int].?,
      "pageSize".as[Int].?,
      "sort".as[List[SortRequest]].?,
      "sortOrder".as[SortingOrder].?,
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
          accessMethod,
          createdDateFrom,
          createdDateTo,
          page,
          pageSize,
          sort,
          sortOrder,
          query,
          includes,
          aggregations
          ) =>
        val itemsParams = ItemsParams(
          items,
          identifiers,
          license,
          locationType,
          accessStatus,
          accessMethod,
          createdDateFrom,
          createdDateTo
        )

        val paginationParams = PaginationParams(page, pageSize, sort, sortOrder)

        parameters(
          "workType".as[FormatFilter] ?,
          "production.dates.from".as[LocalDate].?,
          "production.dates.to".as[LocalDate].?,
          "languages".as[LanguagesFilter].?,
          "archive.category".as[ArchiveCategoryFilter].?,
          "genres.label".as[GenreLabelFilter].?,
          "genres".as[GenreIdFilter].?,
          "subjects.label".as[SubjectLabelFilter].?,
          "subjects".as[SubjectIdFilter].?,
          "contributors.agent.label".as[ContributorsLabelFilter].?,
          "contributors.agent".as[ContributorsIdFilter].?,
          "identifiers".as[IdentifiersFilter].?,
          "partOf".as[PartOfFilter].?,
          "partOf.title".as[PartOfTitleFilter].?,
          "availabilities".as[AvailabilitiesFilter].?,
          "type".as[WorkTypeFilter].?,
          "collection.isRoot".as[CollectionIsRootFilter].?,
          "collection.root".as[CollectionRootFilter].?
        ).tflatMap {
          case (
              format,
              dateFrom,
              dateTo,
              languages,
              archiveCategory,
              genres,
              genreConcepts,
              subjectLabels,
              subjectConcepts,
              contributors,
              contributorsConcepts,
              identifiers,
              partOf,
              partOfTitle,
              availabilities,
              workType,
              collectionIsRoot,
              collectionRoot
              ) =>
            val filterParams = WorkFilterParams(
              format,
              dateFrom,
              dateTo,
              languages,
              archiveCategory,
              genres,
              genreConcepts,
              subjectLabels,
              subjectConcepts,
              contributors,
              contributorsConcepts,
              identifiers,
              partOf,
              partOfTitle,
              availabilities,
              workType,
              collectionIsRoot,
              collectionRoot
            )

            val params = MultipleWorksParams(
              paginationParams = paginationParams,
              itemsParams = itemsParams,
              filterParams = filterParams,
              include = includes,
              aggregations = aggregations,
              query = query
            )
            validated(params.paginationErrors, params)
        }
    }

  implicit val formatFilter: Decoder[FormatFilter] =
    stringListFilter(FormatFilter)

  implicit val workTypeFilter: Decoder[WorkTypeFilter] =
    stringListFilter(WorkTypeFilter)

  implicit val itemLocationTypeIdFilter: Decoder[ItemLocationTypeIdFilter] =
    stringListFilter(ItemLocationTypeIdFilter)

  implicit val languagesFilter: Decoder[LanguagesFilter] =
    stringListFilter(LanguagesFilter)

  implicit val archiveCategoryFilter: Decoder[ArchiveCategoryFilter] =
    stringListFilter(ArchiveCategoryFilter)

  implicit val collectionRootFilter: Decoder[CollectionRootFilter] =
    stringListFilter(CollectionRootFilter)

  implicit val collectionIsRootFilter: Decoder[CollectionIsRootFilter] =
    Decoder.decodeString.emap {
      case "true"  => Right(CollectionIsRootFilter(true))
      case "false" => Right(CollectionIsRootFilter(false))
      case other =>
        Left(s"Got value '$other' with wrong type, expecting 'true' or 'false'")
    }

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

  implicit val accessMethodFilter: Decoder[AccessMethodFilter] =
    validatedStringListFilter(CatalogueAccessMethod.values)(AccessMethodFilter)

  implicit val accessStatusFilter: Decoder[AccessStatusFilter] =
    decodeIncludesAndExcludes(CatalogueAccessStatus.values)
      .emap {
        case IncludesAndExcludes(includes, excludes) =>
          Right(AccessStatusFilter(includes, excludes))
      }

  // These three are exposed so that OpenApiSpecEnumTest can assert the spec's
  // enums still match what these decoders accept.
  val aggregationValues: Seq[(String, WorkAggregationRequest)] = Seq(
    "workType" -> WorkAggregationRequest.Format,
    "genres.label" -> WorkAggregationRequest.GenreLabel,
    "genres" -> WorkAggregationRequest.GenreId,
    "production.dates" -> WorkAggregationRequest.ProductionDate,
    "subjects.label" -> WorkAggregationRequest.SubjectLabel,
    "subjects" -> WorkAggregationRequest.SubjectId,
    "languages" -> WorkAggregationRequest.Languages,
    "archive.category" -> WorkAggregationRequest.ArchiveCategory,
    "collection.root" -> WorkAggregationRequest.CollectionRoot,
    "contributors.agent.label" -> WorkAggregationRequest.ContributorLabel,
    "contributors.agent" -> WorkAggregationRequest.ContributorId,
    "items.locations.license" -> WorkAggregationRequest.License,
    "items.locations.accessConditions.method" -> WorkAggregationRequest.AccessMethod,
    "availabilities" -> WorkAggregationRequest.Availabilities
  )

  val sortValues: Seq[(String, SortRequest)] = Seq(
    "production.dates" -> ProductionDateSortRequest,
    "items.locations.createdDate" -> DigitalLocationCreatedDateSortRequest,
    "collectionPath" -> CollectionPathSortRequest
  )

  val sortOrderValues: Seq[(String, SortingOrder)] = Seq(
    "asc" -> SortingOrder.Ascending,
    "desc" -> SortingOrder.Descending
  )

  implicit val aggregationsDecoder: Decoder[List[WorkAggregationRequest]] =
    decodeOneOfCommaSeparated(aggregationValues: _*)

  implicit val sortDecoder: Decoder[List[SortRequest]] =
    decodeOneOfCommaSeparated(sortValues: _*)

  implicit val sortOrderDecoder: Decoder[SortingOrder] =
    decodeOneOf(sortOrderValues: _*)
}
