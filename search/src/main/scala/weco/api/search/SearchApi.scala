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
  clusterConfig: ElasticConfig,
  additionalElasticClients: Map[String, ResilientElasticClient] = Map.empty,
  additionalClusterConfigs: Map[String, ElasticConfig] = Map.empty,
  implicit val apiConfig: ApiConfig
)(implicit ec: ExecutionContext)
    extends CustomDirectives
    with IdentifierDirectives {

  private val clusterConfigs = Map("default" -> clusterConfig) ++ additionalClusterConfigs
  private val elasticClients = Map("default" -> elasticClient) ++ additionalElasticClients

  private def getControllers[T](
    // we only create the controller if the relevant index is part of the ElasticConfig
    shouldCreate: ElasticConfig => Boolean
  )(getController: (String, ElasticConfig) => T): Map[String, T] =
    clusterConfigs.flatMap {
      case ("default", config) =>
        Some("default" -> getController("default", config))
      case (name, config) if shouldCreate(config) =>
        Some(name -> getController(name, config))
      case _ => None
    }

  private val worksControllers = getControllers(_.worksIndex.isDefined) {
    (name, config) =>
      new WorksController(
        new ElasticsearchService(elasticClients(name)),
        apiConfig,
        worksIndex = config.getWorksIndex,
        semanticConfig = config.semanticConfig
      )
  }

  private val imagesControllers = getControllers(_.imagesIndex.isDefined) {
    (name, config) =>
      new ImagesController(
        new ElasticsearchService(elasticClients(name)),
        apiConfig,
        imagesIndex = config.getImagesIndex
      )
  }

  def routes: Route = handleRejections(rejectionHandler) {
    withRequestTimeoutResponse(request => timeoutResponse) {
      ignoreTrailingSlash {
        parameter("elasticCluster".?) { elasticClusterParam =>
          def routesFor(cluster: String): Route = {
            val worksController = worksControllers.get(cluster)
            val imagesController = imagesControllers.get(cluster)

            buildRoutes(cluster, worksController, imagesController)
          }

          elasticClusterParam match {
            case Some(cluster) if clusterConfigs.contains(cluster) =>
              routesFor(cluster)
            case Some(cluster) =>
              notFound(s"Cluster '$cluster' is not configured")
            // Use default Elasticsearch cluster if `elasticCluster` parameter missing
            case None =>
              routesFor("default")
          }
        }
      }
    }
  }

  private def requireController[T](
    controller: Option[T],
    clusterName: String
  )(handler: T => Route): Route =
    controller match {
      case Some(c) => handler(c)
      case None =>
        notFound(s"Endpoint not available for cluster '$clusterName'")
    }

  private def buildRoutes(
    clusterName: String,
    worksController: Option[WorksController],
    imagesController: Option[ImagesController]
  ): Route =
    concat(
      path("works") {
        requireController(worksController, clusterName) { controller =>
          MultipleWorksParams.parse {
            controller.multipleWorks
          }
        }
      },
      path("works" / Segment) {
        case id if looksLikeCanonicalId(id) =>
          requireController(worksController, clusterName) { controller =>
            SingleWorkParams.parse {
              controller.singleWork(id, _)
            }
          }

        case id =>
          notFound(s"Work not found for identifier $id")
      },
      path("images") {
        requireController(imagesController, clusterName) { controller =>
          MultipleImagesParams.parse {
            controller.multipleImages
          }
        }
      },
      path("images" / Segment) {
        case id if looksLikeCanonicalId(id) =>
          requireController(imagesController, clusterName) { controller =>
            SingleImageParams.parse {
              controller.singleImage(id, _)
            }
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
              requireController(worksController, clusterName) { controller =>
                withFuture {
                  val config = clusterConfigs(clusterName)
                  controller
                    .countWorkTypes(config.getWorksIndex.name)
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
          }
        )
      }
    )

  def getSearchTemplates(clusterName: String): Route = get {
    val config = clusterConfigs(clusterName)
    val worksSearchTemplate = SearchTemplate(
      "multi_matcher_search_query",
      config.getPipelineDate,
      config.getWorksIndex.name,
      WorksTemplateSearchBuilder.queryTemplate
    )

    val imageSearchTemplate = SearchTemplate(
      "image_search_query",
      config.getPipelineDate,
      config.getImagesIndex.name,
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
          "worksIndex" -> config.getWorksIndex.name,
          "imagesIndex" -> config.getImagesIndex.name,
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
