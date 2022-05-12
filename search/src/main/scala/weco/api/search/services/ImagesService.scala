package weco.api.search.services

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import io.circe.Decoder
import io.circe.generic.extras.semiauto._
import weco.api.search.elasticsearch.ElasticsearchService
import weco.api.search.models._
import weco.api.search.models.index.IndexedImage
import weco.json.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

class ImagesService(
  val elasticsearchService: ElasticsearchService,
  queryConfig: QueryConfig
)(
  implicit
  val ec: ExecutionContext
) extends SearchService[IndexedImage, IndexedImage, ImageAggregations, ImageSearchOptions] {

  private val nVisuallySimilarImages = 5

  implicit val decoder: Decoder[IndexedImage] =
    deriveConfiguredDecoder
  implicit val decoderV: Decoder[IndexedImage] =
    decoder

  override protected def createAggregations(
    searchResponse: SearchResponse
  ): Option[ImageAggregations] =
    ImageAggregations(searchResponse)

  override protected val requestBuilder: ImagesRequestBuilder =
    new ImagesRequestBuilder(queryConfig)

  def retrieveSimilarImages(
    index: Index,
    imageId: String,
    similarityMetric: SimilarityMetric = SimilarityMetric.Blended
  ): Future[List[IndexedImage]] = {
    val builder = similarityMetric match {
      case SimilarityMetric.Blended =>
        requestBuilder.requestWithBlendedSimilarity
      case SimilarityMetric.Features =>
        requestBuilder.requestWithSimilarFeatures
      case SimilarityMetric.Colors =>
        requestBuilder.requestWithSimilarColors
    }

    val searchRequest = builder(index, imageId, nVisuallySimilarImages)

    elasticsearchService
      .findBySearch(searchRequest)(decoder)
      .map {
        case Left(_)       => Nil
        case Right(images) => images
      }
  }
}
