package uk.ac.wellcome.platform.api.requests

import akka.http.scaladsl.model.{HttpResponse, RequestEntity, StatusCodes}
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.models.work.generators.ItemsGenerators
import uk.ac.wellcome.platform.api.requests.fixtures.RequestsApiFixture
import weco.api.stacks.services.memory.MemoryItemLookup
import weco.catalogue.internal_model.identifiers.{IdState, IdentifierType}
import weco.catalogue.internal_model.work.Item

class RequestingScenarioTest
    extends AnyFeatureSpec
    with GivenWhenThen
    with Matchers
    with ItemsGenerators
    with RequestsApiFixture
    with IntegrationPatience {

  Feature("requesting an item") {
    Scenario("An item which is not from Sierra") {
      Given("a physical item which is not from Sierra")
      val item = createIdentifiedCalmItem

      val lookup = new MemoryItemLookup(items = Seq(item))

      withRequestsApi(lookup) { _ =>
        When("the user requests the item")

        val path = "/users/1234567/item-requests"

        val entity = createJsonHttpEntityWith(
          s"""
             |{
             |  "itemId": "${item.id.canonicalId}",
             |  "workId": "$createCanonicalId",
             |  "type": "ItemRequest"
             |}
             |""".stripMargin
        )

        val response = waitForPostRequest(path, entity)

        Then("the hold is rejected")
        response.status shouldBe StatusCodes.BadRequest

        And("the error explains why the hold is rejected")
        withStringEntity(response.entity) {
          assertJsonStringsAreEqual(
            _,
            s"""
              |{
              |  "@context": "$contextUrl",
              |  "type": "Error",
              |  "errorType": "http",
              |  "httpStatus": 400,
              |  "label": "Bad Request",
              |  "description": "You cannot request ${item.id.canonicalId}"
              |}
              |""".stripMargin
          )
        }
      }
    }
  }

  def waitForPostRequest(path: String, entity: RequestEntity): HttpResponse =
    whenPostRequestReady(path, entity) { resp =>
      resp
    }

  def createIdentifiedCalmItem: Item[IdState.Identified] =
    createIdentifiedItemWith(
      sourceIdentifier = createSourceIdentifierWith(
        identifierType = IdentifierType.CalmRecordIdentifier,
        ontologyType = "Item"
      ),
      locations = List(createPhysicalLocation)
    )
}
