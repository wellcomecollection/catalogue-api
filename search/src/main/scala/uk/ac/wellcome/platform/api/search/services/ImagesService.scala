package uk.ac.wellcome.platform.api.search.services

import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.Index
import io.circe.Decoder
import uk.ac.wellcome.models.Implicits
import uk.ac.wellcome.platform.api.search.models._
import weco.api.search.elasticsearch.ElasticsearchService
import weco.catalogue.internal_model.image.{Image, ImageState}

import scala.concurrent.{ExecutionContext, Future}

class ImagesService(val elasticsearchService: ElasticsearchService,
                    queryConfig: QueryConfig)(implicit
                                              val ec: ExecutionContext)
    extends SearchService[
      Image[ImageState.Indexed],
      Image[ImageState.Indexed],
      ImageAggregations,
      ImageSearchOptions] {

  private val nVisuallySimilarImages = 5

  // TODO: This isn't ideal, but it's the only way I've been able to get
  // this to compile.  We should move towards named implicits here.
  implicit val decoder: Decoder[Image[ImageState.Indexed]] = Implicits._dec67
  implicit val decoderV: Decoder[Image[ImageState.Indexed]] = Implicits._dec67

  override protected def createAggregations(
    searchResponse: SearchResponse): Option[ImageAggregations] =
    ImageAggregations(searchResponse)

  override protected val requestBuilder: ImagesRequestBuilder =
    new ImagesRequestBuilder(queryConfig)

  def retrieveSimilarImages(index: Index,
                            image: Image[ImageState.Indexed],
                            similarityMetric: SimilarityMetric =
                              SimilarityMetric.Blended)
    : Future[List[Image[ImageState.Indexed]]] = {
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
