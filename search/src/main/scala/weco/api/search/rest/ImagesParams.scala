package weco.api.search.rest

import io.circe.Decoder
import weco.api.search.models._
import weco.api.search.models.request.{
  ImageAggregationRequest,
  ImageInclude,
  MultipleImagesIncludes,
  SingleImageIncludes
}

import scala.util.{Failure, Success}

case class SingleImageParams(
  include: Option[SingleImageIncludes]
) extends QueryParams

object SingleImageParams extends QueryParamsUtils {
  def parse =
    parameter(
      "include".as[SingleImageIncludes].?
    ).tmap {
      case Tuple1(include) => SingleImageParams(include)
    }

  implicit val includesDecoder: Decoder[SingleImageIncludes] =
    decodeOneOfCommaSeparated(
      "visuallySimilar" -> ImageInclude.VisuallySimilar,
      "withSimilarFeatures" -> ImageInclude.WithSimilarFeatures,
      "withSimilarColors" -> ImageInclude.WithSimilarColors,
      "source.contributors" -> ImageInclude.SourceContributors,
      "source.languages" -> ImageInclude.SourceLanguages,
      "source.genres" -> ImageInclude.SourceGenres,
      "source.subjects" -> ImageInclude.SourceSubjects
    ).emap(values => Right(SingleImageIncludes(values: _*)))
}

case class MultipleImagesParams(
  page: Option[Int],
  pageSize: Option[Int],
  query: Option[String],
  license: Option[LicenseFilter],
  `source.contributors.agent.label`: Option[ContributorsFilter],
  `source.genres.label`: Option[GenreFilter],
  `source.genres.concepts`: Option[GenreConceptFilter],
  `source.subjects.label`: Option[SubjectLabelFilter],
  color: Option[ColorMustQuery],
  include: Option[MultipleImagesIncludes],
  aggregations: Option[List[ImageAggregationRequest]]
) extends QueryParams
    with Paginated {

  def searchOptions(apiConfig: ApiConfig): ImageSearchOptions =
    ImageSearchOptions(
      searchQuery = query.map(SearchQuery(_)),
      filters = filters,
      mustQueries = mustQueries,
      aggregations = aggregations.getOrElse(Nil),
      pageSize = pageSize.getOrElse(apiConfig.defaultPageSize),
      pageNumber = page.getOrElse(1)
    )

  private def filters: List[ImageFilter] =
    List(
      license,
      `source.contributors.agent.label`,
      `source.genres.label`,
      `source.genres.concepts`,
      `source.subjects.label`
    ).flatten

  private def mustQueries: List[ImageMustQuery] =
    List(color).flatten
}

object MultipleImagesParams extends QueryParamsUtils {
  import CommonDecoders._

  def parse =
    parameters(
      "page".as[Int].?,
      "pageSize".as[Int].?,
      "query".as[String].?,
      "locations.license".as[LicenseFilter].?,
      "source.contributors.agent.label".as[ContributorsFilter].?,
      "source.genres.label".as[GenreFilter].?,
      "source.genres.concepts".as[GenreConceptFilter].?,
      "source.subjects.label".as[SubjectLabelFilter].?,
      "color".as[ColorMustQuery].?,
      "include".as[MultipleImagesIncludes].?,
      "aggregations".as[List[ImageAggregationRequest]].?
    ).tflatMap { args =>
      val params = (MultipleImagesParams.apply _).tupled(args)
      validated(params.paginationErrors, params)
    }

  implicit val colorMustQuery: Decoder[ColorMustQuery] =
    decodeCommaSeparated.emap { strs =>
      val tryColors = strs.map { s =>
        (s, HsvColor.fromHex(s))
      }

      val colors = tryColors.collect { case (_, Success(c))   => c }
      val unparsed = tryColors.collect { case (s, Failure(_)) => s }

      val errorMessage = unparsed match {
        case Nil => ""
        case Seq(singleColor) =>
          s"'$singleColor' is not a valid value. Please supply a hex string."
        case multipleColors =>
          s"${multipleColors.map(mc => s"'$mc'").mkString(", ")} are not valid values. Please supply hex strings."
      }

      Either.cond(
        unparsed.isEmpty,
        right = ColorMustQuery(colors),
        left = errorMessage
      )
    }

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
      "source.contributors.agent.label" -> ImageAggregationRequest.SourceContributorAgents,
      "source.genres.label" -> ImageAggregationRequest.SourceGenres,
      "source.subjects.label" -> ImageAggregationRequest.SourceSubjects
    )
}
