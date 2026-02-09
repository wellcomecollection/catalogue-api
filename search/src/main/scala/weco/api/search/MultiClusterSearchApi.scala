package weco.api.search

import org.apache.pekko.http.scaladsl.server.Route
import weco.api.search.elasticsearch.{
  ElasticsearchService,
  ResilientElasticClient
}
import weco.api.search.models._
import weco.api.search.rest._
import weco.catalogue.display_model.rest.IdentifierDirectives

import scala.concurrent.ExecutionContext

/**
  * Multi-cluster aware search API router.
  *
  * This allows routing different endpoints to different Elasticsearch clusters,
  * which is useful for:
  * - Experimental indices in separate clusters
  * - Serverless Elasticsearch projects
  * - A/B testing different cluster configurations
  */
class MultiClusterSearchApi(
  // Default cluster client (for existing routes)
  defaultElasticClient: ResilientElasticClient,
  defaultElasticConfig: ElasticConfig,
  // Additional cluster clients mapped by name
  additionalClients: Map[String, ResilientElasticClient],
  additionalClusterConfigs: Map[String, ClusterConfig],
  implicit val apiConfig: ApiConfig
)(implicit ec: ExecutionContext)
    extends ApiRouter
    with CustomDirectives
    with IdentifierDirectives {

  /**
    * Create a controller for a specific cluster.
    * Returns None if the cluster doesn't have the required index configured.
    */
  private def getWorksControllerForCluster(
    clusterName: String
  ): Option[WorksController] =
    for {
      client <- additionalClients.get(clusterName)
      clusterConfig = additionalClusterConfigs(clusterName)
    } yield {
      val semanticConfig = for {
        modelId <- clusterConfig.semanticModelId
        vectorTypeStr <- clusterConfig.semanticVectorType
        vectorType <- vectorTypeStr match {
          case "dense"  => Some(VectorType.Dense)
          case "sparse" => Some(VectorType.Sparse)
          case _        => None
        }
      } yield SemanticConfig(modelId, vectorType)

      new WorksController(
        elasticsearchService = new ElasticsearchService(client),
        apiConfig = apiConfig,
        worksIndex = clusterConfig.worksIndex,
        semanticConfig = semanticConfig
      )
    }

  // Build the default cluster routes inline
  private def defaultClusterRoutes: Route = {
    val defaultApi = new SearchApi(
      elasticClient = defaultElasticClient,
      elasticConfig = defaultElasticConfig,
      apiConfig = apiConfig
    )
    defaultApi.routes
  }

  /** Helper to build list/search routes for a specific cluster */
  private def buildClusterRoutes(clusterName: String,
                                 pathSegment: String): Route =
    path("works" / pathSegment) {
      getWorksControllerForCluster(clusterName) match {
        case Some(controller) =>
          MultipleWorksParams.parse {
            controller.multipleWorks
          }
        case None =>
          notFound(s"Cluster '$clusterName' is not configured")
      }
    }

  def routes: Route = {
    val additionalRoutes = additionalClusterConfigs.keys.toSeq.sorted.map {
      clusterName => buildClusterRoutes(clusterName, clusterName)
    }

    concat(
      (additionalRoutes :+ defaultClusterRoutes): _*
    )
  }
}
