package weco.api.search.rest

import io.circe.Decoder
import weco.api.search.models._
import weco.api.search.models.request.{
  ImageAggregationRequest,
  ImageInclude,
  MultipleImagesIncludes,
  ProductionDateSortRequest,
  SingleImageIncludes,
  SortRequest,
  SortingOrder
}

import java.time.LocalDate
import scala.util.{Failure, Success}

case class SingleImageParams(
  include: Option[SingleImageIncludes]
) extends QueryParams

object SingleImageParams extends QueryParamsUtils {
  def parse =
    parameter(
      "include".as[SingleImageIncludes].?
    ).tmap {
      case Tuple1(include) =>
        SingleImageParams(include)
    }

  implicit val includesDecoder: Decoder[SingleImageIncludes] =
    decodeOneOfCommaSeparated(
      "withSimilarFeatures" -> ImageInclude.WithSimilarFeatures,
      "source.contributors" -> ImageInclude.SourceContributors,
      "source.languages" -> ImageInclude.SourceLanguages,
      "source.genres" -> ImageInclude.SourceGenres,
      "source.subjects" -> ImageInclude.SourceSubjects
    ).emap(values => Right(SingleImageIncludes(values: _*)))
}

case class MultipleImagesParams(
  page: Option[Int],
  pageSize: Option[Int],
  sort: Option[List[SortRequest]],
  sortOrder: Option[SortingOrder],
  query: Option[String],
  license: Option[LicenseFilter],
  `source.contributors.agent.label`: Option[ContributorsLabelFilter],
  `source.contributors.agent`: Option[ContributorsIdFilter],
  `source.genres.label`: Option[GenreLabelFilter],
  `source.genres`: Option[GenreIdFilter],
  `source.subjects.label`: Option[SubjectLabelFilter],
  `source.subjects`: Option[SubjectIdFilter],
  `source.production.dates.from`: Option[LocalDate],
  `source.production.dates.to`: Option[LocalDate],
  color: Option[RgbColor],
  include: Option[MultipleImagesIncludes],
  aggregations: Option[List[ImageAggregationRequest]]
) extends QueryParams
    with Paginated {

  def searchOptions(apiConfig: ApiConfig): ImageSearchOptions =
    ImageSearchOptions(
      searchQuery = query.map(SearchQuery(_)),
      filters = filters,
      color = color,
      aggregations = aggregations.getOrElse(Nil),
      pageSize = pageSize.getOrElse(apiConfig.defaultPageSize),
      pageNumber = page.getOrElse(1),
      sortBy = sort.getOrElse(Nil),
      sortOrder = sortOrder.getOrElse(SortingOrder.Ascending)
    )

  private def filters: List[ImageFilter] =
    List(
      license,
      `source.contributors.agent.label`,
      `source.contributors.agent`,
      `source.genres.label`,
      `source.genres`,
      `source.subjects.label`,
      `source.subjects`,
      dateFilter
    ).flatten

  private def dateFilter: Option[DateRangeFilter] =
    (`source.production.dates.from`, `source.production.dates.to`) match {
      case (None, None)       => None
      case (dateFrom, dateTo) => Some(DateRangeFilter(dateFrom, dateTo))
    }
}

object MultipleImagesParams extends QueryParamsUtils {
  import CommonDecoders._

  def parse =
    parameters(
      "page".as[Int].?,
      "pageSize".as[Int].?,
      "sort".as[List[SortRequest]].?,
      "sortOrder".as[SortingOrder].?,
      "query".as[String].?,
      "locations.license".as[LicenseFilter].?,
      "source.contributors.agent.label".as[ContributorsLabelFilter].?,
      "source.contributors.agent".as[ContributorsIdFilter].?,
      "source.genres.label".as[GenreLabelFilter].?,
      "source.genres".as[GenreIdFilter].?,
      "source.subjects.label".as[SubjectLabelFilter].?,
      "source.subjects".as[SubjectIdFilter].?,
      "source.production.dates.from".as[LocalDate].?,
      "source.production.dates.to".as[LocalDate].?,
      "color".as[RgbColor].?,
      "include".as[MultipleImagesIncludes].?,
      "aggregations".as[List[ImageAggregationRequest]].?
    ).tflatMap { args =>
      val params = (MultipleImagesParams.apply _).tupled(args)
      validated(params.paginationErrors, params)
    }

  implicit val rgbColorDecoder: Decoder[RgbColor] =
    Decoder.decodeString.emap(colorString =>
      RgbColor.fromHex(colorString) match {
        case Success(rgbColor) => Right(rgbColor)
        case Failure(_) =>
          Left(
            s"'$colorString' is not a valid value. Please supply a single hex string."
          )
    })

  implicit val includesDecoder: Decoder[MultipleImagesIncludes] =
    decodeOneOfCommaSeparated(
      "source.contributors" -> ImageInclude.SourceContributors,
      "source.languages" -> ImageInclude.SourceLanguages,
      "source.genres" -> ImageInclude.SourceGenres,
      "source.subjects" -> ImageInclude.SourceSubjects
    ).emap(values => Right(MultipleImagesIncludes(values: _*)))

  implicit val aggregationsDecoder: Decoder[List[ImageAggregationRequest]] =
    decodeOneOfCommaSeparated(
      "locations.license" -> ImageAggregationRequest.License,
      "source.contributors.agent.label" -> ImageAggregationRequest.SourceContributorAgentsLabel,
      "source.contributors.agent" -> ImageAggregationRequest.SourceContributorAgentsId,
      "source.genres.label" -> ImageAggregationRequest.SourceGenresLabel,
      "source.genres" -> ImageAggregationRequest.SourceGenresId,
      "source.subjects.label" -> ImageAggregationRequest.SourceSubjectsLabel,
      "source.subjects" -> ImageAggregationRequest.SourceSubjectsId
    )

  implicit val sortDecoder: Decoder[List[SortRequest]] =
    decodeOneOfCommaSeparated(
      "source.production.dates" -> ProductionDateSortRequest
    )

  implicit val sortOrderDecoder: Decoder[SortingOrder] =
    decodeOneOf(
      "asc" -> SortingOrder.Ascending,
      "desc" -> SortingOrder.Descending
    )
}
