package uk.ac.wellcome.platform.api

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.config.Config
import uk.ac.wellcome.display.ElasticConfig
import uk.ac.wellcome.elasticsearch.typesafe.ElasticBuilder
import uk.ac.wellcome.platform.api.models.{ApiConfig, QueryConfig}
import uk.ac.wellcome.platform.api.swagger.SwaggerDocs
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp

import scala.concurrent.{ExecutionContext, Promise}

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem =
      AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    Tracing.init(config)
    val elasticClient = ElasticBuilder.buildElasticClient(config)

    val elasticConfig = ElasticConfig()

    val apiConfig =
      ApiConfig(
        host = config
          .getStringOption("api.host")
          .getOrElse("localhost"),
        scheme = config
          .getStringOption("api.scheme")
          .getOrElse("https"),
        defaultPageSize = config
          .getIntOption("api.pageSize")
          .getOrElse(10),
        pathPrefix =
          s"${config.getStringOption("api.apiName").getOrElse("catalogue")}",
        contextSuffix = config
          .getStringOption("api.context.suffix")
          .getOrElse("context.json"),
      )

    val queryConfig =
      QueryConfig.fetchFromIndex(elasticClient, elasticConfig.imagesIndex)

    val swaggerDocs = new SwaggerDocs(apiConfig)

    val router = new Router(
      elasticClient = elasticClient,
      elasticConfig = elasticConfig,
      queryConfig = queryConfig,
      swaggerDocs = swaggerDocs,
      apiConfig = apiConfig
    )

    () =>
      Http()
        .bindAndHandle(router.routes, "0.0.0.0", 8888)
        .flatMap(_ => Promise[Done].future)
  }
}
