package weco.api.search.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches._
import com.sksamuel.elastic4s.requests.searches.aggs.TermsAggregation
import com.sksamuel.elastic4s.requests.searches.queries.{
  NoopQuery,
  Query,
  RangeQuery
}
import com.sksamuel.elastic4s.requests.searches.sort._
import io.circe.{Json, JsonObject}
import weco.api.search.models.index.IndexedImage
import weco.api.search.elasticsearch.{ColorQuery, ImageSimilarity}
import weco.api.search.models._
import weco.api.search.models.request.{
  ImageAggregationRequest,
  ProductionDateSortRequest,
  SortingOrder
}
import weco.api.search.rest.PaginationQuery
import weco.api.search.elasticsearch.templateSearch.TemplateSearchRequest

class ImagesRequestBuilder()
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
          includes = Seq("display", "vectorValues.reducedFeatures"),
          aggs = filteredAggregationBuilder(pairables).filteredAggregations,
          preFilter = buildImageFilterQuery(unpairables),
          postFilter = Some(
            must(
              buildImageFilterQuery(searchOptions.filters)
            )
          ),
          knn = searchOptions.color.map(ColorQuery(_))
        )
      )
    )
  }

  private def filteredAggregationBuilder(
    filters: List[ImageFilter with Pairable]
  )(implicit
    searchOptions: ImageSearchOptions
  ) =
    new ImageFiltersAndAggregationsBuilder(
      aggregationRequests = searchOptions.aggregations,
      filters = filters,
      requestToAggregation = toAggregation,
      filterToQuery = buildImageFilterQuery
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

  private def dateOrder(implicit
    searchOptions: ImageSearchOptions
  ): Option[SortingOrder] =
    searchOptions.sortBy collectFirst { case ProductionDateSortRequest =>
      searchOptions.sortOrder
    }

  private def buildImageFilterQuery(filter: ImageFilter): Query =
    filter match {
      case LicenseFilter(licenseIds) =>
        termsQuery(
          field = "filterableValues.locations.license.id",
          values = licenseIds
        )
      case ContributorsFilter(contributorQueries) =>
        termsQuery(
          "filterableValues.source.contributors.agent.label",
          contributorQueries
        )
      case GenreFilter(genreLabels) =>
        termsQuery("filterableValues.source.genres.label", genreLabels)
      case GenreConceptFilter(conceptIds) =>
        if (conceptIds.isEmpty) NoopQuery
        else
          termsQuery("filterableValues.source.genres.concepts.id", conceptIds)
      case SubjectLabelFilter(subjectLabels) =>
        termsQuery("filterableValues.source.subjects.label", subjectLabels)
      case DateRangeFilter(fromDate, toDate) =>
        val (gte, lte) =
          (fromDate map ElasticDate.apply, toDate map ElasticDate.apply)
        RangeQuery(
          "filterableValues.source.production.dates.range.from",
          lte = lte,
          gte = gte
        )
    }

  private def buildImageFilterQuery(filters: Seq[ImageFilter]): Seq[Query] =
    filters.map(buildImageFilterQuery) filter (_ != NoopQuery)

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
