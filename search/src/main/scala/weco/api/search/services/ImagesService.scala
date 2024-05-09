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
  val elasticsearchService: ElasticsearchService
)(implicit
  val ec: ExecutionContext
) extends SearchService[
      IndexedImage,
      IndexedImage,
      ImageAggregations,
      ImageSearchOptions
    ] {

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
    new ImagesRequestBuilder()

  def retrieveSimilarImages(
    index: Index,
    imageId: String,
    image: IndexedImage,
    minScore: Option[Double] = None
  ): Future[List[IndexedImage]] = {
    val builder = requestBuilder.requestWithSimilarFeatures
    // The features metric use KNN which gives a value between 0 and 1.  The ideal threshold value is yet to be determined.
    val defaultMinScore = 0
    val minScoreValue: Double = minScore.getOrElse(defaultMinScore)
    val searchRequest =
      builder(index, imageId, image, nVisuallySimilarImages, minScoreValue)

    elasticsearchService
      .findBySearch(searchRequest)(decoder)
      .map {
        case Left(_) =>
          Nil
        case Right(images) =>
          images
      }
  }
}
