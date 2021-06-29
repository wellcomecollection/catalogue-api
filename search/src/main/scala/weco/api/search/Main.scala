package weco.api.search

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.config.Config
import weco.elasticsearch.typesafe.ElasticBuilder
import weco.Tracing
import weco.api.search.models.{ApiConfig, CheckModel, QueryConfig}
import weco.api.search.swagger.SwaggerDocs
import weco.typesafe.WellcomeTypesafeApp
import weco.typesafe.config.builders.AkkaBuilder
import weco.typesafe.config.builders.EnrichConfig.RichConfig
import weco.catalogue.display_model.ElasticConfig

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

    CheckModel.checkModel(elasticConfig.worksIndex.name)(elasticClient)
    CheckModel.checkModel(elasticConfig.imagesIndex.name)(elasticClient)

    val apiConfig = ApiConfig.build(config)
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
        .bindAndHandle(
          router.routes,
          config.getStringOption("http.host").getOrElse("0.0.0.0"),
          config.getIntOption("http.port").getOrElse(8888)
        )
        .flatMap(_ => Promise[Done].future)
  }
}
