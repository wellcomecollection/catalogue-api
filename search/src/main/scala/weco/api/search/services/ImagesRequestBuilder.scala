package weco.api.search.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches._
import com.sksamuel.elastic4s.requests.searches.queries.{Query, RangeQuery}
import com.sksamuel.elastic4s.requests.searches.sort._
import io.circe.{Json, JsonObject}
import weco.api.search.models.index.IndexedImage
import weco.api.search.elasticsearch.{ColorQuery, ImageSimilarity}
import weco.api.search.models._
import weco.api.search.models.request.{ProductionDateSortRequest, SortingOrder}
import weco.api.search.rest.PaginationQuery
import weco.api.search.elasticsearch.templateSearch.TemplateSearchRequest

object ImagesRequestBuilder
    extends ElasticsearchRequestBuilder[ImageSearchOptions]
    with ImagesTemplateSearchBuilder {

  val idSort: FieldSort = fieldSort("query.id").order(SortOrder.ASC)
  def request(
    searchOptions: ImageSearchOptions,
    index: Index
  ): Right[Nothing, TemplateSearchRequest] = {
    implicit val s: ImageSearchOptions = searchOptions

    val splitFilterRequests = searchOptions.filters.groupBy {
      case _: Pairable => Pairable
      case _           => Unpairable
    }
    val unpairables = splitFilterRequests.getOrElse(Unpairable, Nil)
    val pairables = splitFilterRequests
      .getOrElse(Pairable, Nil)
      .asInstanceOf[List[ImageFilter with Pairable]]

    Right(
      searchRequest(
        indexes = Seq(index.name),
        params = SearchTemplateParams(
          query = searchOptions.searchQuery.map(_.query),
          from = PaginationQuery.safeGetFrom(searchOptions),
          size = searchOptions.pageSize,
          sortByDate = dateOrder,
          sortByScore =
            searchOptions.searchQuery.isDefined || searchOptions.color.isDefined,
          includes = Seq("display", "vectorValues.features"),
          aggs = ImagesAggregationsBuilder
            .getAggregations(pairables, searchOptions.aggregations),
          preFilter = unpairables.collect(buildImageFilterQuery),
          postFilter = Some(
            must(
              searchOptions.filters.collect(buildImageFilterQuery)
            )
          ),
          knn = searchOptions.color.map(ColorQuery(_))
        )
      )
    )
  }

  private def dateOrder(
    implicit
    searchOptions: ImageSearchOptions): Option[SortingOrder] =
    searchOptions.sortBy collectFirst {
      case ProductionDateSortRequest =>
        searchOptions.sortOrder
    }

  val buildImageFilterQuery: PartialFunction[ImageFilter, Query] = {
    case LicenseFilter(licenseIds) =>
      termsQuery(
        field = "filterableValues.locations.license.id",
        values = licenseIds
      )
    case ContributorsLabelFilter(contributorQueries) =>
      termsQuery(
        "filterableValues.source.contributors.agent.label",
        contributorQueries
      )
    case ContributorsIdFilter(conceptIds) =>
      termsQuery(
        "filterableValues.source.contributors.agent.id",
        conceptIds
      )
    case GenreLabelFilter(genreLabels) =>
      termsQuery(
        "filterableValues.source.genres.label",
        genreLabels
      )
    case GenreIdFilter(conceptIds) if conceptIds.nonEmpty =>
      termsQuery(
        "filterableValues.source.genres.concepts.id",
        conceptIds
      )
    case SubjectLabelFilter(subjectLabels) =>
      termsQuery(
        "filterableValues.source.subjects.label",
        subjectLabels
      )
    case SubjectIdFilter(conceptIds) if conceptIds.nonEmpty =>
      termsQuery(
        "filterableValues.source.subjects.concepts.id",
        conceptIds
      )
    case DateRangeFilter(fromDate, toDate) =>
      val (gte, lte) =
        (fromDate map ElasticDate.apply, toDate map ElasticDate.apply)

      RangeQuery(
        "filterableValues.source.production.dates.range.from",
        lte = lte,
        gte = gte
      )
  }

  def requestWithSimilarFeatures
    : (Index, String, IndexedImage, Int, Double) => SearchRequest =
    rawSimilarityRequest(ImageSimilarity.features)

  private def rawSimilarityRequest(
    query: (String, IndexedImage) => JsonObject
  )(
    index: Index,
    imageId: String,
    image: IndexedImage,
    n: Int,
    minScore: Double
  ): SearchRequest =
    search(index).source(
      Json
        .fromJsonObject(
          query(imageId, image)
            .add("min_score", Json.fromDouble(minScore).get)
            .add("size", Json.fromInt(n))
        )
        .noSpaces
    )
}
