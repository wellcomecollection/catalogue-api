package uk.ac.wellcome.platform.api.search

import com.sksamuel.elastic4s.{ElasticDsl, Index}
import com.sksamuel.elastic4s.ElasticDsl._
import org.scalatest.Assertion
import uk.ac.wellcome.fixtures.{fixture, Fixture, RandomGenerators}
import uk.ac.wellcome.platform.api.search.fixtures.ApiFixture

trait ApiTestBase extends ApiFixture with RandomGenerators {
  val apiRoot: String = "https://api-testing.local/catalogue/v2"
  val rootPath: String = s"/${apiConfig.pathPrefix}"
  val contextUrl: String = s"$apiRoot/context.json"

  def emptyJsonResult: String =
    s"""
       |{
       |  ${resultList(totalPages = 0, totalResults = 0)},
       |  "results": []
       |}""".stripMargin

  def badRequest(description: String) =
    s"""{
      "@context": "$contextUrl",
      "type": "Error",
      "errorType": "http",
      "httpStatus": 400,
      "label": "Bad Request",
      "description": "$description"
    }"""

  def goneRequest(description: String) =
    s"""{
      "@context": "$contextUrl",
      "type": "Error",
      "errorType": "http",
      "httpStatus": 410,
      "label": "Gone",
      "description": "$description"
    }"""

  def resultList(
    pageSize: Int = 10,
    totalPages: Int = 1,
    totalResults: Int
  ) =
    s"""
      "@context": "$contextUrl",
      "type": "ResultList",
      "pageSize": $pageSize,
      "totalPages": $totalPages,
      "totalResults": $totalResults
    """

  def notFound(description: String) =
    s"""{
      "@context": "$contextUrl",
      "type": "Error",
      "errorType": "http",
      "httpStatus": 404,
      "label": "Not Found",
      "description": "$description"
    }"""

  def deleted =
    s"""{
      "@context": "$contextUrl",
      "type": "Error",
      "errorType": "http",
      "httpStatus": 410,
      "label": "Gone",
      "description": "This work has been deleted"
    }"""

  def withEmptyIndex[R]: Fixture[Index, R] =
    fixture[Index, R](
      create = {
        val index = createIndex
        elasticClient
          .execute {
            ElasticDsl.createIndex(index.name)
          }
        eventuallyIndexExists(index)
        index
      },
      destroy = eventuallyDeleteIndex
    )

  def assertIsBadRequest(path: String, description: String): Assertion =
    withWorksApi {
      case (_, routes) =>
        assertJsonResponse(routes, path)(
          Status.BadRequest ->
            badRequest(description = description)
        )
    }
}
