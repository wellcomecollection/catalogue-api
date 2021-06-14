package uk.ac.wellcome.platform.api.requests

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.generators.{ItemsGenerators, WorkGenerators}
import uk.ac.wellcome.platform.api.requests.fixtures.RequestsApiFixture
import weco.catalogue.internal_model.identifiers.IdentifierType

class RequestingScenarioTest
  extends AnyFeatureSpec
    with GivenWhenThen
    with Matchers
    with ItemsGenerators
    with WorkGenerators
    with RequestsApiFixture {

  Feature("requesting an item") {
    Scenario("An item which is not from Sierra") {
      Given("a physical item which is not from Sierra")
      val item =
        createIdentifiedItemWith(
          sourceIdentifier = createSourceIdentifierWith(
            identifierType = IdentifierType.CalmRecordIdentifier,
            ontologyType = "Item"
          ),
          locations = List(createPhysicalLocation)
        )

      val work = indexedWork().items(List(item))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, work)

        withRequestsApi(index) { _ =>
          When("the user requests the item")

          val path = "/users/1234567/item-requests"

          val entity = createJsonHttpEntityWith(
            s"""
               |{
               |  "itemId": "${item.id.canonicalId}",
               |  "workId": "${work.state.canonicalId}",
               |  "type": "ItemRequest"
               |}
               |""".stripMargin
          )

          whenPostRequestReady(path, entity) { response =>
            response.status shouldBe StatusCodes.BadRequest

            withStringEntity(response.entity) {
              assertJsonStringsAreEqual(_,
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
    }
  }
}
