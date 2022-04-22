package weco.api.search.rest

import akka.http.scaladsl.server.Route
import cats.implicits._
import com.sksamuel.elastic4s.Index
import weco.Tracing
import weco.api.search.elasticsearch.ElasticsearchService
import weco.api.search.models.{ApiConfig, QueryConfig, SimilarityMetric}
import weco.api.search.services.ImagesService
import weco.catalogue.display_model.models.Implicits._
import weco.catalogue.display_model.models.{
  DisplayImage,
  MultipleImagesIncludes,
  SingleImageIncludes
}
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.{ExecutionContext, Future}

class ImagesController(
  elasticsearchService: ElasticsearchService,
  implicit val apiConfig: ApiConfig,
  imagesIndex: Index,
  queryConfig: QueryConfig
)(implicit ec: ExecutionContext)
    extends CustomDirectives
    with Tracing {

  def singleImage(id: CanonicalId, params: SingleImageParams): Route =
    get {
      withFuture {
        transactFuture("GET /images/{imageId}") {
          val userSpecifiedIndex = params._index.map(Index(_))
          val index = userSpecifiedIndex.getOrElse(imagesIndex)

          imagesService
            .findById(id)(index)
            .flatMap {
              case Right(image) =>
                getSimilarityMetrics(params.include)
                  .traverse { metric =>
                    imagesService
                      .retrieveSimilarImages(index, image, metric)
                      .map(metric -> _)
                  }
                  .map(_.toMap)
                  .map { similarImages =>
                    complete(
                      DisplayImage(
                        image = image,
                        includes =
                          params.include.getOrElse(SingleImageIncludes.none),
                        visuallySimilar =
                          similarImages.get(SimilarityMetric.Blended),
                        withSimilarColors =
                          similarImages.get(SimilarityMetric.Colors),
                        withSimilarFeatures =
                          similarImages.get(SimilarityMetric.Features)
                      )
                    )
                  }

              case Left(err) =>
                Future.successful(
                  elasticError(
                    documentType = "Image",
                    err = err,
                    usingUserSpecifiedIndex = userSpecifiedIndex.isDefined
                  )
                )
            }
        }
      }
    }

  def multipleImages(params: MultipleImagesParams): Route =
    get {
      withFuture {
        transactFuture("GET /images") {
          val searchOptions = params.searchOptions(apiConfig)

          val userSpecifiedIndex = params._index.map(Index(_))
          val index = userSpecifiedIndex.getOrElse(imagesIndex)

          imagesService
            .listOrSearch(index, searchOptions)
            .map {
              case Left(err) =>
                elasticError(
                  documentType = "Image",
                  err = err,
                  usingUserSpecifiedIndex = userSpecifiedIndex.isDefined
                )

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
          if (includes.visuallySimilar) Some(SimilarityMetric.Blended)
          else None,
          if (includes.withSimilarFeatures) Some(SimilarityMetric.Features)
          else None,
          if (includes.withSimilarColors) Some(SimilarityMetric.Colors)
          else None
        ).flatten
      }
      .getOrElse(Nil)

  private lazy val imagesService =
    new ImagesService(elasticsearchService, queryConfig)
}
