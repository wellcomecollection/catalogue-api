package weco.api.search

import akka.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpResponse,
  StatusCodes
}
import akka.http.scaladsl.server.{
  MalformedQueryParamRejection,
  RejectionHandler,
  Route,
  ValidationRejection
}
import com.sksamuel.elastic4s.ElasticClient
import weco.api.search.elasticsearch.ElasticsearchService
import weco.api.search.models._
import weco.api.search.rest._
import weco.api.search.services.{
  ImagesTemplateSearchBuilder,
  WorksTemplateSearchBuilder
}
import weco.catalogue.display_model.rest.IdentifierDirectives
import weco.http.models.DisplayError

import scala.concurrent.ExecutionContext

class SearchApi(
  elasticClient: ElasticClient,
  elasticConfig: ElasticConfig,
  implicit val apiConfig: ApiConfig
)(implicit ec: ExecutionContext)
    extends CustomDirectives
    with IdentifierDirectives {

  def routes: Route = handleRejections(rejectionHandler) {
    withRequestTimeoutResponse(request => timeoutResponse) {
      ignoreTrailingSlash {
        concat(
          path("works") {
            MultipleWorksParams.parse {
              worksController.multipleWorks
            }
          },
          path("works" / Segment) {
            case id if looksLikeCanonicalId(id) =>
              SingleWorkParams.parse {
                worksController.singleWork(id, _)
              }

            case id =>
              notFound(s"Work not found for identifier $id")
          },
          path("images") {
            MultipleImagesParams.parse {
              imagesController.multipleImages
            }
          },
          path("images" / Segment) {
            case id if looksLikeCanonicalId(id) =>
              SingleImageParams.parse {
                imagesController.singleImage(id, _)
              }

            case id => notFound(s"Image not found for identifier $id")
          },
          path("search-templates.json") {
            getSearchTemplates
          },
          path("_elasticConfig") {
            getElasticConfig
          },
          pathPrefix("management") {
            concat(
              path("healthcheck") {
                get {
                  complete("message" -> "ok")
                }
              },
              path("clusterhealth") {
                getClusterHealth
              },
              // This endpoint is meant for the diff tool; it gives a breakdown of the
              // different work types (e.g. Visible, Redirected, Deleted) in the index
              // the API is using.
              //
              // It allows the diff tool to get stats about the API without knowing
              // which ES cluster the API is connecting to or which index it's using.
              path("_workTypes") {
                get {
                  withFuture {
                    worksController
                      .countWorkTypes(elasticConfig.worksIndex)
                      .map {
                        case Right(tally) => complete(tally)
                        case Left(err) =>
                          internalError(
                            new Throwable(s"Error counting work types: $err")
                          )
                      }
                  }
                }
              }
            )
          }
        )
      }
    }
  }

  lazy val elasticsearchService = new ElasticsearchService(elasticClient)

  lazy val worksController =
    new WorksController(
      elasticsearchService,
      apiConfig,
      worksIndex = elasticConfig.worksIndex
    )

  lazy val imagesController =
    new ImagesController(
      elasticsearchService,
      apiConfig,
      imagesIndex = elasticConfig.imagesIndex
    )

  def getClusterHealth: Route =
    get {
      withFuture {
        import com.sksamuel.elastic4s.ElasticDsl._

        elasticClient.execute(clusterHealth()).map { health =>
          complete(health.status)
        }
      }
    }

  def getSearchTemplates: Route = get {
    val worksSearchTemplate = SearchTemplate(
      "multi_matcher_search_query",
      elasticConfig.worksIndex.name,
      WorksTemplateSearchBuilder.queryTemplate
    )

    val imageSearchTemplate = SearchTemplate(
      "image_search_query",
      elasticConfig.imagesIndex.name,
      ImagesTemplateSearchBuilder.queryTemplate
    )

    complete(
      SearchTemplateResponse(
        List(worksSearchTemplate, imageSearchTemplate)
      )
    )
  }

  private def getElasticConfig: Route =
    get {
      complete(
        Map(
          "worksIndex" -> elasticConfig.worksIndex.name,
          "imagesIndex" -> elasticConfig.imagesIndex.name
        )
      )
    }

  val timeoutResponse = HttpResponse(
    StatusCodes.ServiceUnavailable,
    entity = HttpEntity(
      contentType = ContentTypes.`application/json`,
      string = toJson(
        DisplayError(
          statusCode = StatusCodes.ServiceUnavailable,
          description =
            "The server was not able to produce a timely response to your request. Please try again in a short while!"
        )
      )
    )
  )

  def rejectionHandler =
    RejectionHandler.newBuilder
      .handle {
        case MalformedQueryParamRejection(field, msg, _) =>
          invalidRequest(s"$field: $msg")
        case ValidationRejection(msg, _) =>
          invalidRequest(s"$msg")
      }
      .handleNotFound(extractPublicUri { uri =>
        notFound(s"Page not found for URL ${uri.path}")
      })
      .result
}
