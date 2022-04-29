package weco.api.search

import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Index
import io.circe.Json
import org.scalatest.Assertion
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import weco.api.search.fixtures.ApiFixture
import weco.fixtures.LocalResources
import weco.json.JsonUtil._

import scala.util.{Failure, Success, Try}

trait ApiTestBase extends ApiFixture with LocalResources {
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
    s"""{
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

  case class ExampleDocument(id: String, document: Json)

  def getExampleDocuments(ids: Seq[String]): Seq[ExampleDocument] =
    ids.map { id =>
      val doc = Try { readResource(s"example_documents/$id.json") }
        .flatMap(jsonString => fromJson[ExampleDocument](jsonString))

      doc match {
        case Success(f) => f
        case Failure(err) =>
          throw new IllegalArgumentException(
            s"Unable to read example document $id: $err"
          )
      }
    }

  def indexExampleDocuments(
    index: Index,
    ids: String*
  ): Unit = {
    val documents = getExampleDocuments(ids)

    val result = elasticClient.execute(
      bulk(
        documents.map { fixture =>
          indexInto(index.name)
            .id(fixture.id)
            .doc(fixture.document.noSpaces)
        }
      ).refreshImmediately
    )

    // With a large number of works this can take a long time
    // 30 seconds should be enough
    whenReady(result, Timeout(Span(30, Seconds))) { _ =>
      getSizeOf(index) shouldBe documents.size
    }
  }
}
