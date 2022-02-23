package weco.api.requests

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.requests.fixtures.RequestsApiFixture
import weco.catalogue.internal_model.generators.IdentifiersGenerators
import weco.elasticsearch.test.fixtures.ElasticsearchFixtures

import java.time.LocalDate

class RequestsErrorsTest
    extends AnyFunSpec
    with Matchers
    with RequestsApiFixture
    with ElasticsearchFixtures
    with IdentifiersGenerators {
  it("returns a 500 error if the catalogue index doesn't exist") {
    withRequestsApi(elasticClient, index = createIndex) { _ =>
      val path = "/users/1234567/item-requests"
      val neededBy = LocalDate.parse("2022-02-18")
      val entity = createJsonHttpEntityWith(
        s"""
           |{
           |  "itemId": "$createCanonicalId",
           |  "workId": "$createCanonicalId",
           |  "neededBy": "$neededBy",
           |  "type": "ItemRequest"
           |}
           |""".stripMargin
      )

      whenPostRequestReady(path, entity) {
        assertIsDisplayError(_, statusCode = StatusCodes.InternalServerError)
      }
    }
  }
}
