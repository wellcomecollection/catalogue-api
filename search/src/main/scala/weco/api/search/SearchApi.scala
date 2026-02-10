package weco.api.search

import org.apache.pekko.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpResponse,
  StatusCodes
}
import com.sksamuel.elastic4s.ElasticDsl._

import org.apache.pekko.http.scaladsl.server.{
  MalformedQueryParamRejection,
  RejectionHandler,
  Route,
  ValidationRejection
}
import weco.api.search.elasticsearch.{
  ElasticsearchService,
  ResilientElasticClient
}
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
  elasticClient: ResilientElasticClient,
  clusterConfig: ClusterConfig,
  additionalElasticClients: Map[String, ResilientElasticClient] = Map.empty,
  additionalClusterConfigs: Map[String, ClusterConfig] = Map.empty,
  implicit val apiConfig: ApiConfig
)(implicit ec: ExecutionContext)
    extends CustomDirectives
    with IdentifierDirectives {

  private val clusterConfigs = Map("default" -> clusterConfig) ++ additionalClusterConfigs
  private val elasticConfigs = clusterConfigs.map {
    case (name, clusterConfig) =>
      name -> PipelineClusterElasticConfig(clusterConfig)
  }
  private val pipelineDate = elasticConfigs("default").pipelineDate.date
  private val elasticClients = Map("default" -> elasticClient) ++ additionalElasticClients
  private val worksControllers = clusterConfigs.map {
    case (currName, currConfig) =>
      val name = currConfig.worksIndex.map(_ => currName).getOrElse("default")
      currName -> new WorksController(
        new ElasticsearchService(elasticClients(name)),
        apiConfig,
        worksIndex = elasticConfigs(name).worksIndex,
        semanticConfig = clusterConfigs(name).semanticConfig
      )
  }
  private val imagesControllers = clusterConfigs.map {
    case (currName, currConfig) =>
      val name = currConfig.worksIndex.map(_ => currName).getOrElse("default")
      currName -> new ImagesController(
        new ElasticsearchService(elasticClients(name)),
        apiConfig,
        imagesIndex = elasticConfigs(name).imagesIndex
      )
  }

  def routes: Route = handleRejections(rejectionHandler) {
    withRequestTimeoutResponse(request => timeoutResponse) {
      ignoreTrailingSlash {
        parameter("elasticCluster".?) { controllerKey =>
          // Use default ElasticSearch cluster if `elasticCluster` parameter missing
          val key = controllerKey.getOrElse("default")

          worksControllers.get(key) match {
            case Some(worksController) =>
              buildRoutes(key, worksController, imagesControllers(key))
            case None =>
              notFound(s"Cluster '$key' is not configured")
          }
        }
      }
    }
  }

  private def buildRoutes(
    clusterName: String,
    worksController: WorksController,
    imagesController: ImagesController
  ): Route =
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
        getSearchTemplates(clusterName)
      },
      path("_elasticConfig") {
        getElasticConfig(clusterName)
      },
      pathPrefix("management") {
        concat(
          path("healthcheck") {
            get {
              complete("message" -> "ok")
            }
          },
          path("clusterhealth") {
            get {
              withFuture {
                elasticClients(clusterName).execute(clusterHealth()).map {
                  health =>
                    complete(health.status)
                }
              }
            }
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
                  .countWorkTypes(worksController.worksIndex)
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

  def getSearchTemplates(clusterName: String): Route = get {
    val worksSearchTemplate = SearchTemplate(
      "multi_matcher_search_query",
      pipelineDate,
      worksControllers(clusterName).worksIndex.name,
      WorksTemplateSearchBuilder.queryTemplate
    )

    val imageSearchTemplate = SearchTemplate(
      "image_search_query",
      pipelineDate,
      imagesControllers(clusterName).imagesIndex.name,
      ImagesTemplateSearchBuilder.queryTemplate
    )

    complete(
      SearchTemplateResponse(
        List(worksSearchTemplate, imageSearchTemplate)
      )
    )
  }

  private def getElasticConfig(clusterName: String): Route =
    get {
      complete(
        Map(
          "worksIndex" -> worksControllers(clusterName).worksIndex.name,
          "imagesIndex" -> imagesControllers(clusterName).imagesIndex.name,
          "pipelineDate" -> pipelineDate,
          "clusterName" -> clusterName
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
