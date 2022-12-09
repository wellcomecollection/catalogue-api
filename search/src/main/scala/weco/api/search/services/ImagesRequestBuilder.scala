package weco.api.search.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches._
import com.sksamuel.elastic4s.requests.searches.aggs.TermsAggregation
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.sort._
import io.circe.{Json, JsonObject}

import weco.api.search.models.index.IndexedImage
import weco.api.search.elasticsearch.{
  ColorQuery,
  ImageSimilarity,
  ImagesMultiMatcher
}
import weco.api.search.models._
import weco.api.search.models.request.ImageAggregationRequest
import weco.api.search.rest.PaginationQuery

class ImagesRequestBuilder(queryConfig: QueryConfig)
    extends ElasticsearchRequestBuilder[ImageSearchOptions] {

  val idSort: FieldSort = fieldSort("query.id").order(SortOrder.ASC)

  lazy val colorQuery = new ColorQuery(
    binSizes = queryConfig.paletteBinSizes,
    binMinima = queryConfig.paletteBinMinima
  )

  def request(searchOptions: ImageSearchOptions, index: Index): SearchRequest =
    search(index)
      .aggs { filteredAggregationBuilder(searchOptions).filteredAggregations }
      .query(
        searchQuery(searchOptions)
          .filter(
            buildImageFilterQuery(searchOptions.filters)
          )
      )
      .sortBy { sortBy(searchOptions) }
      .limit(searchOptions.pageSize)
      .from(PaginationQuery.safeGetFrom(searchOptions))
      .sourceInclude(
        "display",
        // we do KNN searches for similar images, and for that we need
        // to send the image's vectors to Elasticsearch
        "query.inferredData.reducedFeatures"
      )

  private def filteredAggregationBuilder(searchOptions: ImageSearchOptions) =
    new ImageFiltersAndAggregationsBuilder(
      aggregationRequests = searchOptions.aggregations,
      filters = searchOptions.filters,
      requestToAggregation = toAggregation,
      filterToQuery = buildImageFilterQuery,
      searchQuery = searchQuery(searchOptions)
    )

  private def searchQuery(searchOptions: ImageSearchOptions): BoolQuery =
    searchOptions.searchQuery
      .map { q =>
        ImagesMultiMatcher(q.query)
      }
      .getOrElse(boolQuery)
      .must(
        buildImageMustQuery(searchOptions.mustQueries)
      )

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

  private def sortBy(searchOptions: ImageSearchOptions): Seq[Sort] =
    if (searchOptions.searchQuery.isDefined || searchOptions.mustQueries.nonEmpty) {
      List(scoreSort(SortOrder.DESC), idSort)
    } else {
      List(idSort)
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
      case SubjectLabelFilter(subjectLabels) =>
        termsQuery("query.source.subjects.label.keyword", subjectLabels)
    }

  private def buildImageFilterQuery(filters: Seq[ImageFilter]): Seq[Query] =
    filters.map { buildImageFilterQuery }

  private def buildImageMustQuery(queries: List[ImageMustQuery]): Seq[Query] =
    queries.map {
      case ColorMustQuery(hexColors) =>
        colorQuery(field = "query.inferredData.palette", hexColors)
    }

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
            // n is the number of similar documents we want the query to return
            // However, because this is used by KNN searches, which also return
            // the document in question (which is later filtered out of the result list),
            // an extra slot must be added for the source document to occupy.
            .add("size", Json.fromInt(n + 1))
        )
        .noSpaces
    )
}
