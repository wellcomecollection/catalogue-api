package uk.ac.wellcome.platform.api.search

import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.server.{
  MalformedQueryParamRejection,
  RejectionHandler,
  Route,
  ValidationRejection
}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import uk.ac.wellcome.platform.api.search.elasticsearch.{
  ImagesMultiMatcher,
  WorksMultiMatcher
}
import uk.ac.wellcome.platform.api.rest.CustomDirectives
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.platform.api.search.models.{
  QueryConfig,
  SearchTemplate,
  SearchTemplateResponse
}
import uk.ac.wellcome.platform.api.search.rest.{
  ImagesController,
  MultipleImagesParams,
  MultipleWorksParams,
  SingleImageParams,
  SingleWorkParams,
  WorksController
}
import uk.ac.wellcome.platform.api.search.swagger.SwaggerDocs
import weco.api.search.elasticsearch.ElasticsearchService
import weco.catalogue.display_model.ElasticConfig
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.ExecutionContext
import scala.util.{Success, Try}

class Router(
  elasticClient: ElasticClient,
  elasticConfig: ElasticConfig,
  queryConfig: QueryConfig,
  swaggerDocs: SwaggerDocs,
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
        path("context.json") {
          getFromResource("context-v2.json")
        },
        path("swagger.json") {
          swagger
        },
        path("search-templates.json") {
          getSearchTemplates
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

  def swagger: Route = get {
    complete(
      HttpEntity(MediaTypes.`application/json`, swaggerDocs.json)
    )
  }

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
