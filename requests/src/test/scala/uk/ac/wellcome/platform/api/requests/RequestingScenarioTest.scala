package uk.ac.wellcome.platform.api.requests

import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.{
  HttpRequest,
  HttpResponse,
  RequestEntity,
  StatusCodes,
  Uri
}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.models.work.generators.ItemsGenerators
import uk.ac.wellcome.platform.api.common.services.SierraService
import uk.ac.wellcome.platform.api.requests.fixtures.RequestsApiFixture
import weco.api.stacks.services.ItemLookup
import weco.api.stacks.services.memory.MemoryItemLookup
import weco.catalogue.internal_model.identifiers.{IdState, IdentifierType}
import weco.catalogue.internal_model.work.Item
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}

class RequestingScenarioTest
    extends AnyFeatureSpec
    with GivenWhenThen
    with Matchers
    with ItemsGenerators
    with RequestsApiFixture
    with IntegrationPatience
    with ScalatestRouteTest {

  Feature("requesting an item") {
    Scenario("An item which is not from Sierra") {
      Given("a physical item which is not from Sierra")
      val item = createIdentifiedCalmItem

      val lookup = new MemoryItemLookup(items = Seq(item))
      implicit val route: Route = createRoute(lookup)

      When("the user requests the item")
      val response = makePostRequest(
        path = "/users/1234567/item-requests",
        entity = createJsonHttpEntityWith(
          s"""
             |{
             |  "itemId": "${item.id.canonicalId}",
             |  "workId": "$createCanonicalId",
             |  "type": "ItemRequest"
             |}
             |""".stripMargin
        )
      )

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

    Scenario("An item which does not exist") {
      val lookup = new MemoryItemLookup(items = Seq.empty)
      implicit val route: Route = createRoute(lookup)

      Given("an item ID that doesn't exist")
      val itemId = createCanonicalId

      When("the user requests a non-existent item")
      val response = makePostRequest(
        path = "/users/1234567/item-requests",
        entity = createJsonHttpEntityWith(
          s"""
             |{
             |  "itemId": "$itemId",
             |  "workId": "$createCanonicalId",
             |  "type": "ItemRequest"
             |}
             |""".stripMargin
        )
      )

      Then("the API returns a 404 error")
      response.status shouldBe StatusCodes.NotFound

      And("the error explains why the hold is rejected")
      withStringEntity(response.entity) {
        assertJsonStringsAreEqual(
          _,
          s"""
             |{
             |  "@context": "$contextUrl",
             |  "type": "Error",
             |  "errorType": "http",
             |  "httpStatus": 404,
             |  "label": "Not Found",
             |  "description": "Item not found for identifier $itemId"
             |}
             |""".stripMargin
        )
      }
    }
  }

  def makePostRequest(path: String, entity: RequestEntity)(
    implicit route: Route): HttpResponse = {
    val request = HttpRequest(
      method = POST,
      uri = s"https://localhost$path",
      entity = entity
    )

    request ~> route ~> check {
      response
    }
  }

  def createIdentifiedCalmItem: Item[IdState.Identified] =
    createIdentifiedItemWith(
      sourceIdentifier = createSourceIdentifierWith(
        identifierType = IdentifierType.CalmRecordIdentifier,
        ontologyType = "Item"
      ),
      locations = List(createPhysicalLocation)
    )

  def createRoute(
    itemLookup: ItemLookup,
    responses: Seq[(HttpRequest, HttpResponse)] = Seq()): Route = {
    val client = new MemoryHttpClient(responses) with HttpGet with HttpPost {
      override val baseUri: Uri = Uri("http://localhost:1234")
    }

    val api: RequestsApi = new RequestsApi(
      sierraService = SierraService(client),
      itemLookup = itemLookup
    )

    api.routes
  }
}
