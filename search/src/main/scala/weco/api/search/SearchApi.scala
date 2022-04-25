package weco.api.search

import akka.http.scaladsl.server.{
  MalformedQueryParamRejection,
  RejectionHandler,
  Route,
  ValidationRejection
}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import weco.api.search.elasticsearch.{
  ElasticsearchService,
  ImagesMultiMatcher,
  WorksMultiMatcher
}
import weco.api.search.models.{
  ApiConfig,
  ElasticConfig,
  QueryConfig,
  SearchTemplate,
  SearchTemplateResponse
}
import weco.api.search.rest._
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.ExecutionContext
import scala.util.{Success, Try}

class SearchApi(
  elasticClient: ElasticClient,
  elasticConfig: ElasticConfig,
  queryConfig: QueryConfig,
  implicit val apiConfig: ApiConfig
)(implicit ec: ExecutionContext)
    extends CustomDirectives {

  def routes: Route = handleRejections(rejectionHandler) {
    ignoreTrailingSlash {
      concat(
        path("works") {
          MultipleWorksParams.parse { worksController.multipleWorks }
        },
        path("works" / Segment) { id: String =>
          Try { CanonicalId(id) } match {
            case Success(workId) =>
              SingleWorkParams.parse {
                worksController.singleWork(workId, _)
              }

            case _ => notFound(s"Work not found for identifier $id")
          }
        },
        path("images") {
          MultipleImagesParams.parse { imagesController.multipleImages }
        },
        path("images" / Segment) { id: String =>
          Try { CanonicalId(id) } match {
            case Success(imageId) =>
              SingleImageParams.parse {
                imagesController.singleImage(imageId, _)
              }

            case _ => notFound(s"Image not found for identifier $id")
          }
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
                  worksController.countWorkTypes(elasticConfig.worksIndex).map {
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
      imagesIndex = elasticConfig.imagesIndex,
      queryConfig
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
      WorksMultiMatcher("{{query}}")
        .filter(termQuery(field = "type", value = "Visible"))
    )

    val imageSearchTemplate = SearchTemplate(
      "image_search_query",
      elasticConfig.imagesIndex.name,
      ImagesMultiMatcher("{{query}}")
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
