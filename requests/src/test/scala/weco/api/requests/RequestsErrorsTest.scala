package weco.api.requests

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.requests.fixtures.RequestsApiFixture
import weco.catalogue.internal_model.generators.IdentifiersGenerators
import weco.elasticsearch.test.fixtures.ElasticsearchFixtures

class RequestsErrorsTest extends AnyFunSpec with Matchers with RequestsApiFixture with ElasticsearchFixtures with IdentifiersGenerators {
  it("returns a 500 error if the catalogue index doesn't exist") {
    withRequestsApi(elasticClient, index = createIndex) { _ =>
      val path = "/users/1234567/item-requests"
      val entity = createJsonHttpEntityWith(
        s"""
           |{
           |  "itemId": "$createCanonicalId",
           |  "workId": "$createCanonicalId",
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
