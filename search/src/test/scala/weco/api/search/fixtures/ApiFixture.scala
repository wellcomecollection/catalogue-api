package weco.api.search.fixtures

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.{ContentTypes, StatusCode, Uri}
import akka.http.scaladsl.model.headers.Host
import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import io.circe.parser.parse
import io.circe.Json
import org.scalatest.funspec.AnyFunSpec
import weco.api.search.Router
import weco.fixtures.TestWith
import weco.catalogue.internal_model.index.IndexFixtures
import weco.api.search.models.{ApiConfig, QueryConfig}
import weco.api.search.swagger.SwaggerDocs
import weco.catalogue.display_model.ElasticConfig

trait ApiFixture extends AnyFunSpec with ScalatestRouteTest with IndexFixtures {

  val Status = akka.http.scaladsl.model.StatusCodes

  val publicRootUri: String

  implicit def defaultHostInfo: DefaultHostInfo = DefaultHostInfo(
    host = Host(apiConfig.publicHost),
    securedConnection = if (apiConfig.publicScheme == "https") true else false
  )

  lazy val apiConfig = ApiConfig(
    publicRootUri = Uri(publicRootUri),
    defaultPageSize = 10,
    contextSuffix = "context.json"
  )

  // Note: creating new instances of the SwaggerDocs class is expensive, so
  // we cache it and reuse it between test instances to reduce the number
  // of times we have to create it.
  lazy val swaggerDocs = new SwaggerDocs(apiConfig)

  private def withRouter[R](
    elasticConfig: ElasticConfig
  )(testWith: TestWith[Route, R]): R = {
    val router = new Router(
      elasticClient,
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

  def assertJsonResponse(
    routes: Route,
    path: String,
    unordered: Boolean = false
  )(expectedResponse: (StatusCode, String)) =
    eventually {
      expectedResponse match {
        case (expectedStatus, expectedJson) =>
          Get(path) ~> routes ~> check {
            contentType shouldEqual ContentTypes.`application/json`
            parseJson(responseAs[String], unordered) shouldEqual parseJson(
              expectedJson,
              unordered
            )
            status shouldEqual expectedStatus
          }
      }
    }

  def assertRedirectResponse(routes: Route, path: String)(
    expectedResponse: (StatusCode, String)
  ) =
    eventually {
      expectedResponse match {
        case (expectedStatus, expectedLocation) =>
          Get(path) ~> routes ~> check {
            status shouldEqual expectedStatus
            header("Location").map(_.value) shouldEqual Some(expectedLocation)
          }
      }
    }

  def parseJson(string: String, unordered: Boolean = false): Json =
    parse(string) match {
      case Right(json) => sortedJson(unordered)(json)
      case Left(err) =>
        throw new RuntimeException(
          s"Asked to compare a string that wasn't JSON. Error: $err. JSON:\n$string"
        )
    }

  def sortedJson(unordered: Boolean)(json: Json): Json =
    json.arrayOrObject(
      json,
      array => {
        val arr = array.map(sortedJson(unordered))
        if (unordered) {
          Json.arr(arr.sortBy(_.toString): _*)
        } else {
          Json.arr(arr: _*)
        }
      },
      obj =>
        Json.obj(
          obj.toList
            .map {
              case (key, value) =>
                (key, sortedJson(unordered)(value))
            }
            .sortBy(tup => tup._1): _*
      )
    )
}
