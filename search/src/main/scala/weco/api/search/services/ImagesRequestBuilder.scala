package weco.api.search.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches._
import com.sksamuel.elastic4s.requests.searches.aggs.TermsAggregation
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.queries.{
  NoopQuery,
  Query,
  RangeQuery
}
import com.sksamuel.elastic4s.requests.searches.sort._
import io.circe.{Json, JsonObject}
import weco.api.search.models.index.IndexedImage
import weco.api.search.elasticsearch.{
  ColorQuery,
  ImageSimilarity,
  ImagesMultiMatcher
}
import weco.api.search.models._
import weco.api.search.models.request.{
  ImageAggregationRequest,
  ProductionDateSortRequest,
  SortingOrder
}
import weco.api.search.rest.PaginationQuery

class ImagesRequestBuilder()
    extends ElasticsearchRequestBuilder[ImageSearchOptions] {

  val idSort: FieldSort = fieldSort("query.id").order(SortOrder.ASC)
  def request(
    searchOptions: ImageSearchOptions,
    index: Index
  ): Left[SearchRequest, Nothing] =
    Left(
      search(index)
        .aggs { filteredAggregationBuilder(searchOptions).filteredAggregations }
        .query(searchQuery(searchOptions))
        .copy(knn = searchOptions.color.map(ColorQuery(_)))
        .sortBy { sortBy(searchOptions) }
        .limit(searchOptions.pageSize)
        .from(PaginationQuery.safeGetFrom(searchOptions))
        .sourceInclude(
          "display",
          // we do KNN searches for similar images, and for that we need
          // to send the image's vectors to Elasticsearch
          "query.inferredData.reducedFeatures"
        )
        .postFilter {
          must(buildImageFilterQuery(searchOptions.filters))
        }
    )

  private def filteredAggregationBuilder(searchOptions: ImageSearchOptions) =
    new ImageFiltersAndAggregationsBuilder(
      aggregationRequests = searchOptions.aggregations,
      filters = searchOptions.filters,
      requestToAggregation = toAggregation,
      filterToQuery = buildImageFilterQuery
    )

  private def searchQuery(searchOptions: ImageSearchOptions): BoolQuery =
    searchOptions.searchQuery
      .map { q =>
        ImagesMultiMatcher(q.query)
      }
      .getOrElse(boolQuery)

  private def toAggregation(aggReq: ImageAggregationRequest) = aggReq match {
    // Note: we want these aggregations to return every possible value, so we
    // want this to be as many licenses as we support in the catalogue pipeline.
    //
    // At time of writing (May 2022), we have 11 different licenses; I've used
    // 20 here so we have some headroom if we add new licenses in future.
    case ImageAggregationRequest.License =>
      TermsAggregation("license")
        .size(20)
        .field("aggregatableValues.locations.license")

    case ImageAggregationRequest.SourceContributorAgents =>
      TermsAggregation("sourceContributorAgents")
        .size(20)
        .field("aggregatableValues.source.contributors.agent.label")

    case ImageAggregationRequest.SourceGenres =>
      TermsAggregation("sourceGenres")
        .size(20)
        .field("aggregatableValues.source.genres.label")

    case ImageAggregationRequest.SourceSubjects =>
      TermsAggregation("sourceSubjects")
        .size(20)
        .field("aggregatableValues.source.subjects.label")
  }

  private def sortBy(implicit searchOptions: ImageSearchOptions): Seq[Sort] =
    if (searchOptions.searchQuery.isDefined || searchOptions.color.isDefined) {
      sort :+ scoreSort(SortOrder.DESC) :+ idSort
    } else {
      sort :+ idSort
    }

  private def sort(implicit searchOptions: ImageSearchOptions) =
    searchOptions.sortBy
      .map {
        case ProductionDateSortRequest =>
          "query.source.production.dates.range.from"
      }
      .map {
        FieldSort(_).order(sortOrder)
      }

  private def sortOrder(implicit searchOptions: ImageSearchOptions) =
    searchOptions.sortOrder match {
      case SortingOrder.Ascending  => SortOrder.ASC
      case SortingOrder.Descending => SortOrder.DESC
    }

  private def buildImageFilterQuery(filter: ImageFilter): Query =
    filter match {
      case LicenseFilter(licenseIds) =>
        termsQuery(field = "query.locations.license.id", values = licenseIds)
      case ContributorsFilter(contributorQueries) =>
        termsQuery(
          "query.source.contributors.agent.label.keyword",
          contributorQueries
        )
      case GenreFilter(genreLabels) =>
        termsQuery("query.source.genres.label.keyword", genreLabels)
      case GenreConceptFilter(conceptIds) =>
        if (conceptIds.isEmpty) NoopQuery
        else termsQuery("query.source.genres.concepts.id", conceptIds)
      case SubjectLabelFilter(subjectLabels) =>
        termsQuery("query.source.subjects.label.keyword", subjectLabels)
      case DateRangeFilter(fromDate, toDate) =>
        val (gte, lte) =
          (fromDate map ElasticDate.apply, toDate map ElasticDate.apply)
        RangeQuery(
          "query.source.production.dates.range.from",
          lte = lte,
          gte = gte
        )
    }

  private def buildImageFilterQuery(filters: Seq[ImageFilter]): Seq[Query] =
    filters.map { buildImageFilterQuery } filter (_ != NoopQuery)

  def requestWithBlendedSimilarity
    : (Index, String, IndexedImage, Int, Double) => SearchRequest =
    rawSimilarityRequest(ImageSimilarity.blended)

  def requestWithSimilarFeatures
    : (Index, String, IndexedImage, Int, Double) => SearchRequest =
    rawSimilarityRequest(ImageSimilarity.features)

  def requestWithSimilarColors
    : (Index, String, IndexedImage, Int, Double) => SearchRequest =
    similarityRequest(ImageSimilarity.color)

  private def similarityRequest(
    query: (String, IndexedImage, Index) => Query
  )(
    index: Index,
    imageId: String,
    image: IndexedImage,
    n: Int,
    minScore: Double
  ): SearchRequest =
    search(index)
      .size(n)
      .minScore(minScore)
      .query(query(imageId, image, index))

  private def rawSimilarityRequest(
    query: (String, IndexedImage, Index) => JsonObject
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
          query(imageId, image, index)
            .add("min_score", Json.fromDouble(minScore).get)
            .add("size", Json.fromInt(n))
        )
        .noSpaces
    )
}
