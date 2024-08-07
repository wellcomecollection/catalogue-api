package weco.api.search.rest

import org.apache.pekko.http.scaladsl.server.Route
import cats.implicits._
import com.sksamuel.elastic4s.Index
import weco.Tracing
import weco.api.search.elasticsearch.ElasticsearchService
import weco.api.search.json.CatalogueJsonUtil
import weco.api.search.models.request.{
  MultipleImagesIncludes,
  SingleImageIncludes
}
import weco.api.search.models.{ApiConfig, SimilarityMetric}
import weco.api.search.services.ImagesService

import scala.concurrent.{ExecutionContext, Future}

class ImagesController(
  elasticsearchService: ElasticsearchService,
  implicit val apiConfig: ApiConfig,
  imagesIndex: Index
)(implicit ec: ExecutionContext)
    extends CustomDirectives
    with CatalogueJsonUtil
    with Tracing {

  def singleImage(id: String, params: SingleImageParams): Route =
    get {
      withFuture {
        transactFuture("GET /images/{imageId}") {
          imagesService
            .findById(id)(imagesIndex)
            .flatMap {
              case Right(image) =>
                getSimilarityMetrics(params.include)
                  .traverse { metric =>
                    imagesService
                      .retrieveSimilarImages(imagesIndex, id, image)
                      .map(metric -> _)
                  }
                  .map(_.toMap)
                  .map { similarImages =>
                    complete(
                      image.display.asJson(
                        includes =
                          params.include.getOrElse(SingleImageIncludes.none),
                        withSimilarFeatures =
                          similarImages.get(SimilarityMetric.Features)
                      )
                    )
                  }

              case Left(err) =>
                Future.successful(elasticError(documentType = "Image", err))
            }
        }
      }
    }

  def multipleImages(params: MultipleImagesParams): Route =
    get {
      withFuture {
        transactFuture("GET /images") {
          val searchOptions = params.searchOptions(apiConfig)

          imagesService
            .listOrSearch(imagesIndex, searchOptions)
            .map {
              case Left(err) => elasticError(documentType = "Image", err)

              case Right(resultList) =>
                extractPublicUri { uri =>
                  complete(
                    DisplayResultList(
                      resultList = resultList,
                      searchOptions = searchOptions,
                      includes =
                        params.include.getOrElse(MultipleImagesIncludes.none),
                      requestUri = uri
                    )
                  )
                }
            }
        }
      }
    }

  private def getSimilarityMetrics(
    maybeIncludes: Option[SingleImageIncludes]
  ): List[SimilarityMetric] =
    maybeIncludes
      .map { includes =>
        List(
          if (includes.withSimilarFeatures) Some(SimilarityMetric.Features)
          else None
        ).flatten
      }
      .getOrElse(Nil)

  private lazy val imagesService =
    new ImagesService(elasticsearchService)
}
