package weco.api.search

import org.apache.pekko.http.scaladsl.server.Route
import org.scalatest.{Assertion, Suite}
import weco.api.search.fixtures.ApiFixture

import scala.math

trait ApiTestBase extends ApiFixture {
  this: Suite =>
  val publicRootUri = "https://api-testing.local/catalogue/v2"

  // This is the path relative to which requests are made on the host,
  // not necessarily the expected public path! When it is empty, requests
  // are made to the root of the host API.
  val rootPath: String = ""

  def emptyJsonResult: String =
    """
      |{
      |  "type": "ResultList",
      |  "pageSize": 10,
      |  "totalPages": 0,
      |  "totalResults": 0,
      |  "results": []
      |}
      |""".stripMargin

  def badRequest(description: String) =
    s"""{
      "type": "Error",
      "errorType": "http",
      "httpStatus": 400,
      "label": "Bad Request",
      "description": "$description"
    }"""

  def goneRequest(description: String) =
    s"""{
      "type": "Error",
      "errorType": "http",
      "httpStatus": 410,
      "label": "Gone",
      "description": "$description"
    }"""

  def resultListWithCalculatedPageCount(totalResults: Int, pageSize: Int = 10): String =
    resultList(
      pageSize,
      totalPages = math.ceil(totalResults.toDouble / pageSize).toInt,
      totalResults = totalResults
    )

  def resultList(
    pageSize: Int = 10,
    totalPages: Int = 1,
    totalResults: Int
  ) =
    s"""
      "type": "ResultList",
      "pageSize": $pageSize,
      "totalPages": $totalPages,
      "totalResults": $totalResults
    """

  def notFound(description: String) =
    s"""{
      "type": "Error",
      "errorType": "http",
      "httpStatus": 404,
      "label": "Not Found",
      "description": "$description"
    }"""

  def deleted =
    """{
      "type": "Error",
      "errorType": "http",
      "httpStatus": 410,
      "label": "Gone",
      "description": "This work has been deleted"
    }"""

  def assertIsBadRequest(path: String, description: String): Assertion =
    withWorksApi {
      case (_, routes) =>
        assertJsonResponse(routes, path)(
          Status.BadRequest ->
            badRequest(description = description)
        )
    }

  def assertBadRequest(
    route: Route
  )(path: String, description: String): Assertion =
    assertJsonResponse(route, path)(
      Status.BadRequest -> badRequest(description = description)
    )

  def assertNotFound(
    route: Route
  )(path: String, description: String): Assertion =
    assertJsonResponse(route, path)(
      Status.NotFound -> notFound(description = description)
    )
}
