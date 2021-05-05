package weco.api.search.fixtures

import akka.http.scaladsl.model.{ContentTypes, StatusCode, Uri}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.circe.Json
import io.circe.parser.parse
import org.scalatest.Assertion
import org.scalatest.funspec.AnyFunSpec
import uk.ac.wellcome.models.index.IndexFixtures
import uk.ac.wellcome.platform.api.models.ApiConfig

trait ResponseFixtures extends AnyFunSpec with ScalatestRouteTest with IndexFixtures {
  val publicRootUri: String

  lazy val apiConfig = ApiConfig(
    publicRootUri = Uri(publicRootUri),
    defaultPageSize = 10,
    contextSuffix = "context.json"
  )

  def assertJsonResponse(
    routes: Route,
    path: String,
    unordered: Boolean = false
  )(expectedResponse: (StatusCode, String)): Assertion =
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
  ): Assertion =
    eventually {
      expectedResponse match {
        case (expectedStatus, expectedLocation) =>
          Get(path) ~> routes ~> check {
            status shouldEqual expectedStatus
            header("Location").map(_.value) shouldEqual Some(expectedLocation)
          }
      }
    }

  private def parseJson(string: String, unordered: Boolean): Json =
    parse(string) match {
      case Right(json) => sortedJson(unordered)(json)
      case Left(err) =>
        throw new RuntimeException(
          s"Asked to compare a string that wasn't JSON. Error: $err. JSON:\n$string"
        )
    }

  private def sortedJson(unordered: Boolean)(json: Json): Json =
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
