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
  private val elasticClients = Map("default" -> elasticClient) ++ additionalElasticClients

  // Always create default works controller; only create additional cluster controllers if worksIndex is defined
  private val worksControllers = clusterConfigs.flatMap {
    case ("default", config) =>
      Some(
        "default" -> new WorksController(
          new ElasticsearchService(elasticClients("default")),
          apiConfig,
          worksIndex = config.getWorksIndex,
          semanticConfig = config.semanticConfig
        ))
    case (name, config) if config.worksIndex.isDefined =>
      Some(
        name -> new WorksController(
          new ElasticsearchService(elasticClients(name)),
          apiConfig,
          worksIndex = config.getWorksIndex,
          semanticConfig = config.semanticConfig
        ))
    case _ => None
  }

  // Always create default images controller; only create additional cluster controllers if imagesIndex is defined
  private val imagesControllers = clusterConfigs.flatMap {
    case ("default", config) =>
      Some(
        "default" -> new ImagesController(
          new ElasticsearchService(elasticClients("default")),
          apiConfig,
          imagesIndex = config.getImagesIndex
        ))
    case (name, config) if config.imagesIndex.isDefined =>
      Some(
        name -> new ImagesController(
          new ElasticsearchService(elasticClients(name)),
          apiConfig,
          imagesIndex = config.getImagesIndex
        ))
    case _ => None
  }

  def routes: Route = handleRejections(rejectionHandler) {
    withRequestTimeoutResponse(request => timeoutResponse) {
      ignoreTrailingSlash {
        parameter("elasticCluster".?) { controllerKey =>
          def routesFor(key: String): Route = {
            val worksController = worksControllers.get(key)
            val imagesController = imagesControllers.get(key)

            buildRoutes(key, worksController, imagesController)
          }

          controllerKey match {
            case Some(key) if clusterConfigs.contains(key) =>
              routesFor(key)
            case Some(key) =>
              notFound(s"Cluster '$key' is not configured")
            // Use default Elasticsearch cluster if `elasticCluster` parameter missing
            case None =>
              routesFor("default")
          }
        }
      }
    }
  }

  private def buildRoutes(
    clusterName: String,
    worksController: Option[WorksController],
    imagesController: Option[ImagesController]
  ): Route =
    concat(
      path("works") {
        worksController match {
          case Some(controller) =>
            MultipleWorksParams.parse {
              controller.multipleWorks
            }
          case None =>
            notFound(s"Endpoint not available for cluster '$clusterName'")
        }
      },
      path("works" / Segment) {
        case id if looksLikeCanonicalId(id) =>
          worksController match {
            case Some(controller) =>
              SingleWorkParams.parse {
                controller.singleWork(id, _)
              }
            case None =>
              notFound(s"Endpoint not available for cluster '$clusterName'")
          }

        case id =>
          notFound(s"Work not found for identifier $id")
      },
      path("images") {
        imagesController match {
          case Some(controller) =>
            MultipleImagesParams.parse {
              controller.multipleImages
            }
          case None =>
            notFound(s"Endpoint not available for cluster '$clusterName'")
        }
      },
      path("images" / Segment) {
        case id if looksLikeCanonicalId(id) =>
          imagesController match {
            case Some(controller) =>
              SingleImageParams.parse {
                controller.singleImage(id, _)
              }
            case None =>
              notFound(s"Endpoint not available for cluster '$clusterName'")
          }

        case id => notFound(s"Image not found for identifier $id")
      },
      path("search-templates.json") {
        (worksController, imagesController) match {
          case (Some(_), Some(_)) =>
            getSearchTemplates(clusterName)
          case _ =>
            notFound(s"Endpoint not available for cluster '$clusterName'")
        }
      },
      path("_elasticConfig") {
        (worksController, imagesController) match {
          case (Some(_), Some(_)) =>
            getElasticConfig(clusterName)
          case _ =>
            notFound(s"Endpoint not available for cluster '$clusterName'")
        }
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
              worksController match {
                case Some(controller) =>
                  withFuture {
                    controller
                      .countWorkTypes(controller.worksIndex)
                      .map {
                        case Right(tally) => complete(tally)
                        case Left(err) =>
                          internalError(
                            new Throwable(s"Error counting work types: $err")
                          )
                      }
                  }
                case None =>
                  notFound(s"Endpoint not available for cluster '$clusterName'")
              }
            }
          }
        )
      }
    )

  def getSearchTemplates(clusterName: String): Route = get {
    val config = clusterConfigs(clusterName)
    val worksSearchTemplate = SearchTemplate(
      "multi_matcher_search_query",
      config.getPipelineDate,
      worksControllers(clusterName).worksIndex.name,
      WorksTemplateSearchBuilder.queryTemplate
    )

    val imageSearchTemplate = SearchTemplate(
      "image_search_query",
      config.getPipelineDate,
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
      val config = clusterConfigs(clusterName)
      complete(
        Map(
          "worksIndex" -> worksControllers(clusterName).worksIndex.name,
          "imagesIndex" -> imagesControllers(clusterName).imagesIndex.name,
          "pipelineDate" -> config.getPipelineDate,
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
