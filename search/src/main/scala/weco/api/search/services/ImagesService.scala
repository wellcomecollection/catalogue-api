package weco.api.search.services

import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.Index
import io.circe.Decoder
import weco.catalogue.internal_model.Implicits
import weco.api.search.elasticsearch.ElasticsearchService
import weco.api.search.models._
import weco.catalogue.internal_model.image.{Image, ImageState}

import scala.concurrent.{ExecutionContext, Future}

class ImagesService(
  val elasticsearchService: ElasticsearchService,
  queryConfig: QueryConfig
)(
  implicit
  val ec: ExecutionContext
) extends SearchService[Image[ImageState.Indexed], Image[ImageState.Indexed], ImageAggregations, ImageSearchOptions] {

  private val nVisuallySimilarImages = 5

  implicit val decoder: Decoder[Image[ImageState.Indexed]] =
    Implicits._decImageIndexed
  implicit val decoderV: Decoder[Image[ImageState.Indexed]] =
    Implicits._decImageIndexed

  override protected def createAggregations(
    searchResponse: SearchResponse
  ): Option[ImageAggregations] =
    ImageAggregations(searchResponse)

  override protected val requestBuilder: ImagesRequestBuilder =
    new ImagesRequestBuilder(queryConfig)

  def retrieveSimilarImages(
    index: Index,
    image: Image[ImageState.Indexed],
    similarityMetric: SimilarityMetric = SimilarityMetric.Blended
  ): Future[List[Image[ImageState.Indexed]]] = {
    val builder = similarityMetric match {
      case SimilarityMetric.Blended =>
        requestBuilder.requestWithBlendedSimilarity
      case SimilarityMetric.Features =>
        requestBuilder.requestWithSimilarFeatures
      case SimilarityMetric.Colors =>
        requestBuilder.requestWithSimilarColors
    }

    val searchRequest = builder(index, image.id, nVisuallySimilarImages)

    elasticsearchService
      .findBySearch(searchRequest)(decoder)
      .map {
        case Left(_)       => Nil
        case Right(images) => images
      }
  }
}
