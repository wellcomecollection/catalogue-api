package uk.ac.wellcome.platform.api.search.fixtures

import akka.http.scaladsl.model.headers.Host
import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import org.scalatest.funspec.AnyFunSpec
import uk.ac.wellcome.api.display.ElasticConfig
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.platform.api.search.Router
import uk.ac.wellcome.platform.api.search.models.QueryConfig
import uk.ac.wellcome.platform.api.search.swagger.SwaggerDocs
import weco.api.search.fixtures.ResponseFixtures

trait ApiFixture extends AnyFunSpec with ResponseFixtures {
  val Status = akka.http.scaladsl.model.StatusCodes

  val publicRootUri: String

  implicit def defaultHostInfo: DefaultHostInfo = DefaultHostInfo(
    host = Host(apiConfig.publicHost),
    securedConnection = if (apiConfig.publicScheme == "https") true else false
  )

  // Note: creating new instances of the SwaggerDocs class is expensive, so
  // we cache it and reuse it between test instances to reduce the number
  // of times we have to create it.
  lazy val swaggerDocs = new SwaggerDocs(apiConfig)

  private def withRouter[R](
    elasticConfig: ElasticConfig
  )(testWith: TestWith[Route, R]): R = {
    val router = new Router(
      elasticConfig,
      QueryConfig(
        paletteBinSizes = Seq(Seq(4, 6, 9), Seq(2, 4, 6), Seq(1, 3, 5)),
        paletteBinMinima = Seq(0f, 10f / 256, 10f / 256)
      ),
      swaggerDocs = swaggerDocs,
      apiConfig = apiConfig
    )

    testWith(router.routes)
  }

  def withApi[R](testWith: TestWith[Route, R]): R = {
    val elasticConfig = ElasticConfig(
      worksIndex = Index("worksIndex-notused"),
      imagesIndex = Index("imagesIndex-notused")
    )

    withRouter(elasticConfig) { route =>
      testWith(route)
    }
  }

  def withWorksApi[R](testWith: TestWith[(Index, Route), R]): R =
    withLocalWorksIndex { worksIndex =>
      val elasticConfig = ElasticConfig(
        worksIndex = worksIndex,
        imagesIndex = Index("imagesIndex-notused")
      )

      withRouter(elasticConfig) { route =>
        testWith((worksIndex, route))
      }
    }

  def withImagesApi[R](testWith: TestWith[(Index, Route), R]): R =
    withLocalImagesIndex { imagesIndex =>
      val elasticConfig = ElasticConfig(
        worksIndex = Index("worksIndex-notused"),
        imagesIndex = imagesIndex
      )

      withRouter(elasticConfig) { route =>
        testWith((imagesIndex, route))
      }
    }
}
