package uk.ac.wellcome.platform.api.rest

import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import uk.ac.wellcome.display.models._
import uk.ac.wellcome.display.models.Implicits._
import uk.ac.wellcome.platform.api.Tracing
import uk.ac.wellcome.platform.api.models.{
  ApiConfig,
  QueryConfig,
  SimilarityMetric
}
import uk.ac.wellcome.platform.api.services.{
  ElasticsearchService,
  ImagesService
}
import cats.implicits._
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.{ExecutionContext, Future}

class ImagesController(elasticsearchService: ElasticsearchService,
                       implicit val apiConfig: ApiConfig,
                       imagesIndex: Index,
                       queryConfig: QueryConfig)(implicit ec: ExecutionContext)
    extends CustomDirectives
    with Tracing
    with FailFastCirceSupport {
  import DisplayResultList.encoder
  import ResultResponse.encoder

  def singleImage(id: CanonicalId, params: SingleImageParams): Route =
    getWithFuture {
      transactFuture("GET /images/{imageId}") {
        val index =
          params._index.map(Index(_)).getOrElse(imagesIndex)
        imagesService
          .findImageById(id)(index)
          .flatMap {
            case Right(Some(image)) =>
              getSimilarityMetrics(params.include)
                .traverse { metric =>
                  imagesService
                    .retrieveSimilarImages(index, image, metric)
                    .map(metric -> _)
                }
                .map(_.toMap)
                .map { similarImages =>
                  complete(
                    ResultResponse(
                      context = contextUri,
                      result = DisplayImage(
                        image = image,
                        includes =
                          params.include.getOrElse(SingleImageIncludes.none),
                        visuallySimilar =
                          similarImages.get(SimilarityMetric.Blended),
                        withSimilarColors =
                          similarImages.get(SimilarityMetric.Colors),
                        withSimilarFeatures =
                          similarImages.get(SimilarityMetric.Features),
                      )
                    )
                  )
                }
            case Right(None) =>
              Future.successful(notFound(s"Image not found for identifier $id"))
            case Left(err) => Future.successful(elasticError(err))
          }
      }
    }

  def multipleImages(params: MultipleImagesParams): Route =
    getWithFuture {
      transactFuture("GET /images") {
        val searchOptions = params.searchOptions(apiConfig)
        val index =
          params._index.map(Index(_)).getOrElse(imagesIndex)
        imagesService
          .listOrSearchImages(index, searchOptions)
          .map {
            case Left(err) => elasticError(err)
            case Right(resultList) =>
              extractPublicUri { uri =>
                complete(
                  DisplayResultList(
                    resultList = resultList,
                    searchOptions = searchOptions,
                    includes =
                      params.include.getOrElse(MultipleImagesIncludes.none),
                    requestUri = uri,
                    contextUri = contextUri
                  )
                )
              }
          }
      }
    }

  private def getSimilarityMetrics(
    maybeIncludes: Option[SingleImageIncludes]): List[SimilarityMetric] =
    maybeIncludes
      .map { includes =>
        List(
          if (includes.visuallySimilar) Some(SimilarityMetric.Blended)
          else None,
          if (includes.withSimilarFeatures) Some(SimilarityMetric.Features)
          else None,
          if (includes.withSimilarColors) Some(SimilarityMetric.Colors)
          else None,
        ).flatten
      }
      .getOrElse(Nil)

  private lazy val imagesService =
    new ImagesService(elasticsearchService, queryConfig)
}
