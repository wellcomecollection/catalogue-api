package weco.api.requests

import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpMethods,
  HttpRequest,
  HttpResponse,
  RequestEntity,
  StatusCodes,
  Uri
}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.sksamuel.elastic4s.Index
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import weco.api.requests.fixtures.RequestsApiFixture
import weco.api.requests.services.{
  ItemLookup,
  RequestsService,
  SierraRequestsService
}
import weco.catalogue.internal_model.work.generators.{
  ItemsGenerators,
  WorkGenerators
}
import weco.catalogue.internal_model.identifiers.{IdState, IdentifierType}
import weco.catalogue.internal_model.index.IndexFixtures
import weco.catalogue.internal_model.work.Item
import weco.sierra.generators.SierraIdentifierGenerators
import weco.sierra.models.identifiers.{SierraItemNumber, SierraPatronNumber}
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}
import weco.catalogue.internal_model.Implicits._

class RequestingScenarioTest
    extends AnyFeatureSpec
    with GivenWhenThen
    with Matchers
    with ItemsGenerators
    with WorkGenerators
    with IndexFixtures
    with RequestsApiFixture
    with IntegrationPatience
    with ScalatestRouteTest
    with SierraIdentifierGenerators {

  Feature("requesting an item") {
    Scenario("An item which is not from Sierra") {
      Given("a physical item which is not from Sierra")
      val item = createIdentifiedCalmItem
      val work = indexedWork().items(List(item))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, work)

        implicit val route: Route = createRoute(index)

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

    Scenario("An item which does not exist") {
      withLocalWorksIndex { index =>
        implicit val route: Route = createRoute(index)

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

    Scenario("An item that exists and can be ordered") {
      Given("a physical item from Sierra")
      val patronNumber = createSierraPatronNumber
      val itemNumber = createSierraItemNumber
      val item = createIdentifiedSierraItemWith(itemNumber)
      val work = indexedWork().items(List(item))

      val responses = Seq(
        (
          createHoldRequest(patronNumber, itemNumber),
          HttpResponse(status = StatusCodes.NoContent)
        )
      )

      withLocalWorksIndex { index =>
        implicit val route: Route = createRoute(index, responses)
        insertIntoElasticsearch(index, work)

        When("the user requests the item")
        val response = makePostRequest(
          path = s"/users/$patronNumber/item-requests",
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

        Then("the API returns an Accepted response")
        response.status shouldBe StatusCodes.Accepted
        response.status.intValue shouldBe 202

        And("an empty body")
        response.entity shouldBe HttpEntity.Empty
      }
    }

    Scenario("An item that exists and cannot be ordered") {
      Given("a physical item from Sierra")
      val patronNumber = createSierraPatronNumber
      val itemNumber = createSierraItemNumber
      val item = createIdentifiedSierraItemWith(itemNumber)
      val work = indexedWork().items(List(item))

      val responses = Seq(
        (
          createHoldRequest(patronNumber, itemNumber),
          HttpResponse(
            status = StatusCodes.InternalServerError,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                 |{
                 |  "code": 132,
                 |  "specificCode": 2,
                 |  "httpStatus": 500,
                 |  "name": "XCirc error",
                 |  "description": "XCirc error : This record is not available"
                 |}
                 |""".stripMargin
            )
          )
        ),
        (
          createListHoldsRequest(patronNumber),
          createListHoldsResponse(patronNumber, items = Seq())
        ),
        (
          createItemRequest(itemNumber),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                |{
                |  "total": 1,
                |  "start": 0,
                |  "entries": [
                |    {
                |      "id": "$itemNumber",
                |      "deleted": false,
                |      "suppressed": false,
                |      "status": {
                |        "code": "-",
                |        "display": "Available"
                |      },
                |      "holdCount": 0,
                |      "fixedFields": {
                |        "61": {
                |          "label": "I TYPE",
                |          "value": "4",
                |          "display": "serial"
                |        },
                |        "79": {
                |          "label": "LOCATION",
                |          "value": "sgser",
                |          "display": "Closed stores journals"
                |        },
                |        "88": {
                |          "label": "STATUS",
                |          "value": "-",
                |          "display": "Available"
                |        },
                |        "97": {
                |          "label": "IMESSAGE",
                |          "value": " "
                |        },
                |        "108": {
                |          "label": "OPACMSG",
                |          "value": "n",
                |          "display": "Manual request"
                |        }
                |      }
                |    }
                |  ]
                |}
                |""".stripMargin
            )
          )
        )
      )

      withLocalWorksIndex { index =>
        implicit val route: Route = createRoute(index, responses)
        insertIntoElasticsearch(index, work)

        When("the user requests the item")
        val response = makePostRequest(
          path = s"/users/$patronNumber/item-requests",
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
        response.status.intValue shouldBe 400

        And("the error explains why the hold is rejected")
        withStringEntity(response.entity) {
          assertJsonStringsAreEqual(
            _,
            s"""
               |{
               |  "type": "Error",
               |  "errorType": "http",
               |  "httpStatus": 400,
               |  "label": "Bad Request",
               |  "description": "You can't request ${item.id.canonicalId}"
               |}
               |""".stripMargin
          )
        }
      }
    }

    Scenario("An item that this user has already requested") {
      Given("a physical item from Sierra")
      val patronNumber = createSierraPatronNumber
      val itemNumber = createSierraItemNumber
      val item = createIdentifiedSierraItemWith(itemNumber)
      val work = indexedWork().items(List(item))

      val responses = Seq(
        (
          createHoldRequest(patronNumber, itemNumber),
          HttpResponse(
            status = StatusCodes.InternalServerError,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                |{
                |  "code": 132,
                |  "specificCode": 2,
                |  "httpStatus": 500,
                |  "name": "XCirc error",
                |  "description": "XCirc error : This record is not available"
                |}
                |""".stripMargin
            )
          )
        ),
        (
          createListHoldsRequest(patronNumber),
          createListHoldsResponse(patronNumber, items = Seq(itemNumber))
        )
      )

      withLocalWorksIndex { index =>
        implicit val route: Route = createRoute(index, responses)
        insertIntoElasticsearch(index, work)

        When("the user requests the item")
        val response = makePostRequest(
          path = s"/users/$patronNumber/item-requests",
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

        Then("the API returns an Accepted response")
        response.status shouldBe StatusCodes.Accepted
        response.status.intValue shouldBe 202

        And("an empty body")
        response.entity shouldBe HttpEntity.Empty
      }
    }

    Scenario("An item that is ordered twice in quick succession") {
      // I discovered this scenario by accident, when experimenting with the
      // Sierra API -- if you send the same `POST /patrons/{userId}/requests`
      // in quick succession, you get this 500 error with a 929 code.
      Given("a physical item from Sierra")
      val patronNumber = createSierraPatronNumber
      val itemNumber = createSierraItemNumber
      val item = createIdentifiedSierraItemWith(itemNumber)
      val work = indexedWork().items(List(item))

      And("which has just been requested in Sierra")
      val responses = Seq(
        (
          createHoldRequest(patronNumber, itemNumber),
          HttpResponse(
            status = StatusCodes.InternalServerError,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                 |{
                 |  "code": 132,
                 |  "specificCode": 929,
                 |  "httpStatus": 500,
                 |  "name": "XCirc error",
                 |  "description": "XCirc error : Your request has already been sent."
                 |}
                 |""".stripMargin
            )
          )
        ),
        (
          createListHoldsRequest(patronNumber),
          createListHoldsResponse(patronNumber, items = Seq(itemNumber))
        )
      )

      withLocalWorksIndex { index =>
        implicit val route: Route = createRoute(index, responses)
        insertIntoElasticsearch(index, work)

        When("the user requests the item")
        val response = makePostRequest(
          path = s"/users/$patronNumber/item-requests",
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

        Then("the API returns an Accepted response")
        response.status shouldBe StatusCodes.Accepted
        response.status.intValue shouldBe 202

        And("an empty body")
        response.entity shouldBe HttpEntity.Empty
      }
    }

    Scenario("An item that is on hold for another user") {
      Given("a physical item from Sierra")
      val patronNumber = createSierraPatronNumber
      val itemNumber = createSierraItemNumber
      val item = createIdentifiedSierraItemWith(itemNumber)
      val work = indexedWork().items(List(item))

      And("which is on hold for another user")
      val responses = Seq(
        (
          createHoldRequest(patronNumber, itemNumber),
          HttpResponse(
            status = StatusCodes.InternalServerError,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                 |{
                 |  "code": 132,
                 |  "specificCode": 2,
                 |  "httpStatus": 500,
                 |  "name": "XCirc error",
                 |  "description": "XCirc error : This record is not available"
                 |}
                 |""".stripMargin
            )
          )
        ),
        (
          createListHoldsRequest(patronNumber),
          createListHoldsResponse(patronNumber, items = Seq())
        ),
        (
          createItemRequest(itemNumber),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                |{
                |  "total": 1,
                |  "start": 0,
                |  "entries": [
                |    {
                |      "id": "$itemNumber",
                |      "deleted": false,
                |      "suppressed": false,
                |      "fixedFields": { },
                |      "holdCount": 1
                |    }
                |  ]
                |}
                |""".stripMargin
            )
          )
        )
      )

      withLocalWorksIndex { index =>
        implicit val route: Route = createRoute(index, responses)
        insertIntoElasticsearch(index, work)

        When("the user requests the item")
        val response = makePostRequest(
          path = s"/users/$patronNumber/item-requests",
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

        Then("the API returns a Conflict response")
        response.status shouldBe StatusCodes.Conflict
        response.status.intValue shouldBe 409

        And("the error explains why the hold is rejected")
        withStringEntity(response.entity) {
          assertJsonStringsAreEqual(
            _,
            s"""
               |{
               |  "type": "Error",
               |  "errorType": "http",
               |  "httpStatus": 409,
               |  "label": "Conflict",
               |  "description": "Item ${item.id.canonicalId} is on hold for another library member"
               |}
               |""".stripMargin
          )
        }
      }
    }

    Scenario("A user at their hold limit") {
      Given("a physical item from Sierra")
      val patronNumber = createSierraPatronNumber
      val itemNumber = createSierraItemNumber
      val item = createIdentifiedSierraItemWith(itemNumber)
      val work = indexedWork().items(List(item))

      And("a user who has as many items as they're allowed to request")
      val holdLimit = 10

      val responses = Seq(
        (
          createHoldRequest(patronNumber, itemNumber),
          HttpResponse(
            status = StatusCodes.InternalServerError,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                |{
                |  "code": 132,
                |  "specificCode": 2,
                |  "httpStatus": 500,
                |  "name": "XCirc error",
                |  "description": "XCirc error : There is a problem with your library record.  Please see a librarian."
                |}
                |""".stripMargin
            )
          )
        ),
        (
          createListHoldsRequest(patronNumber),
          createListHoldsResponse(patronNumber, items = (1 to holdLimit).map {
            _ =>
              createSierraItemNumber
          })
        )
      )

      withLocalWorksIndex { index =>
        implicit val route: Route =
          createRoute(index, responses, holdLimit = holdLimit)
        insertIntoElasticsearch(index, work)

        When("the user requests the item")
        val response = makePostRequest(
          path = s"/users/$patronNumber/item-requests",
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

        Then("the API returns a Forbidden response")
        response.status shouldBe StatusCodes.Forbidden
        response.status.intValue shouldBe 403

        And("the body explains the error")
        withStringEntity(response.entity) {
          assertJsonStringsAreEqual(
            _,
            s"""
               |{
               |  "type": "Error",
               |  "errorType": "http",
               |  "httpStatus": 403,
               |  "label": "Forbidden",
               |  "description": "You're at your account limit and you cannot request more items"
               |}
               |""".stripMargin
          )
        }
      }
    }

    Scenario("An item that has been deleted in Sierra") {
      // This scenario is relatively unlikely in practice -- once an item gets deleted
      // in Sierra, it should be removed from the catalogue API within a few minutes.
      //
      // However, there could be a longer delay if there's a problem in the pipeline
      // which prevents updates from being processed.
      //
      // We return a 400 Bad Request instead of a 404 Not Found because from the
      // perspective of wc.org/works, this item does exist -- it just can't be requested.

      Given("a physical item from Sierra in the catalogue API")
      val patronNumber = createSierraPatronNumber
      val itemNumber = createSierraItemNumber
      val item = createIdentifiedSierraItemWith(itemNumber)
      val work = indexedWork().items(List(item))

      And("which is deleted in Sierra")
      val responses = Seq(
        (
          createHoldRequest(patronNumber, itemNumber),
          HttpResponse(
            status = StatusCodes.InternalServerError,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                |{
                |  "code": 132,
                |  "specificCode": 433,
                |  "httpStatus": 500,
                |  "name": "XCirc error",
                |  "description": "XCirc error : Bib record cannot be loaded"
                |}
                |""".stripMargin
            )
          )
        ),
        (
          createItemRequest(itemNumber),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                 |{
                 |  "total": 1,
                 |  "start": 0,
                 |  "entries": [
                 |    {
                 |      "id": "$itemNumber",
                 |      "deletedDate": "2001-01-01",
                 |      "deleted": true
                 |    }
                 |  ]
                 |}
                 |""".stripMargin
            )
          )
        )
      )

      withLocalWorksIndex { index =>
        implicit val route: Route = createRoute(index, responses)
        insertIntoElasticsearch(index, work)

        When("the user requests the item")
        val response = makePostRequest(
          path = s"/users/$patronNumber/item-requests",
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
        response.status.intValue shouldBe 400

        And("the error explains why the hold is rejected")
        withStringEntity(response.entity) {
          assertJsonStringsAreEqual(
            _,
            s"""
               |{
               |  "type": "Error",
               |  "errorType": "http",
               |  "httpStatus": 400,
               |  "label": "Bad Request",
               |  "description": "You can't request ${item.id.canonicalId}"
               |}
               |""".stripMargin
          )
        }
      }
    }

    Scenario("An item that has been suppressed in Sierra") {
      // Like the previous scenario, this should be unlikely in practice, but we include
      // the test for completeness.

      Given("a physical item from Sierra in the catalogue API")
      val patronNumber = createSierraPatronNumber
      val itemNumber = createSierraItemNumber
      val item = createIdentifiedSierraItemWith(itemNumber)
      val work = indexedWork().items(List(item))

      And("which is suppressed in Sierra")
      val responses = Seq(
        (
          createHoldRequest(patronNumber, itemNumber),
          HttpResponse(
            status = StatusCodes.InternalServerError,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                |{
                |  "code": 132,
                |  "specificCode": 2,
                |  "httpStatus": 500,
                |  "name": "XCirc error",
                |  "description": "XCirc error : This record is not available"
                |}
                |""".stripMargin
            )
          )
        ),
        (
          createListHoldsRequest(patronNumber),
          createListHoldsResponse(patronNumber, items = Seq())
        ),
        (
          createItemRequest(itemNumber),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                 |{
                 |  "total": 1,
                 |  "start": 0,
                 |  "entries": [
                 |    {
                 |      "id": "$itemNumber",
                 |      "deletedDate": "2001-01-01",
                 |      "deleted": false,
                 |      "suppressed": true,
                 |      "holdCount": 0,
                 |      "fixedFields": { }
                 |    }
                 |  ]
                 |}
                 |""".stripMargin
            )
          )
        )
      )

      withLocalWorksIndex { index =>
        implicit val route: Route = createRoute(index, responses)
        insertIntoElasticsearch(index, work)

        When("the user requests the item")
        val response = makePostRequest(
          path = s"/users/$patronNumber/item-requests",
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
        response.status.intValue shouldBe 400

        And("the error explains why the hold is rejected")
        withStringEntity(response.entity) {
          assertJsonStringsAreEqual(
            _,
            s"""
               |{
               |  "type": "Error",
               |  "errorType": "http",
               |  "httpStatus": 400,
               |  "label": "Bad Request",
               |  "description": "You can't request ${item.id.canonicalId}"
               |}
               |""".stripMargin
          )
        }
      }
    }

    Scenario("An item that doesn't exist in Sierra") {
      // This should never happen -- when an item gets deleted in Sierra, the record
      // still hangs around with "deleted: true".  If this happens, something has
      // gone very wrong.

      Given("a physical item from Sierra in the catalogue API")
      val patronNumber = createSierraPatronNumber
      val itemNumber = createSierraItemNumber
      val item = createIdentifiedSierraItemWith(itemNumber)
      val work = indexedWork().items(List(item))

      And("which doesn't exist in Sierra")
      val responses = Seq(
        (
          createHoldRequest(patronNumber, itemNumber),
          HttpResponse(
            status = StatusCodes.InternalServerError,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                |{
                |  "code": 132,
                |  "specificCode": 433,
                |  "httpStatus": 500,
                |  "name": "XCirc error",
                |  "description": "XCirc error : Bib record cannot be loaded"
                |}
                |""".stripMargin
            )
          )
        ),
        (
          createItemRequest(itemNumber),
          HttpResponse(
            status = StatusCodes.NotFound,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                 |{
                 |  "code": 107,
                 |  "specificCode": 0,
                 |  "httpStatus": 404,
                 |  "name": "Record not found"
                 |}
                 |""".stripMargin
            )
          )
        )
      )

      withLocalWorksIndex { index =>
        implicit val route: Route = createRoute(index, responses)
        insertIntoElasticsearch(index, work)

        When("the user requests the item")
        val response = makePostRequest(
          path = s"/users/$patronNumber/item-requests",
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

        Then("we throw an internal server error")
        response.status shouldBe StatusCodes.InternalServerError
        response.status.intValue shouldBe 500

        And("we display a generic response")
        assertIsDisplayError(
          response,
          statusCode = StatusCodes.InternalServerError
        )
      }
    }

    Scenario("A user that doesn't exist in Sierra") {
      // This should never happen -- when an item gets deleted in Sierra, the record
      // still hangs around with "deleted: true".  If this happens, something has
      // gone very wrong.

      Given("a physical item from Sierra in the catalogue API")
      val itemNumber = createSierraItemNumber
      val item = createIdentifiedSierraItemWith(itemNumber)
      val work = indexedWork().items(List(item))

      And("and a user that doesn't exist in Sierra")
      val patronNumber = createSierraPatronNumber
      val responses = Seq(
        (
          createHoldRequest(patronNumber, itemNumber),
          HttpResponse(
            status = StatusCodes.NotFound,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                |{
                |  "code": 107,
                |  "specificCode": 0,
                |  "httpStatus": 404,
                |  "name": "Record not found"
                |}
                |""".stripMargin
            )
          )
        )
      )

      withLocalWorksIndex { index =>
        implicit val route: Route = createRoute(index, responses)
        insertIntoElasticsearch(index, work)

        When("the user requests the item")
        val response = makePostRequest(
          path = s"/users/$patronNumber/item-requests",
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

        Then("we return a Not Found response")
        response.status shouldBe StatusCodes.NotFound
        response.status.intValue shouldBe 404

        And("the error explains the problem")
        withStringEntity(response.entity) {
          assertJsonStringsAreEqual(
            _,
            s"""
               |{
               |  "type": "Error",
               |  "errorType": "http",
               |  "httpStatus": 404,
               |  "label": "Not Found",
               |  "description": "There is no such user $patronNumber"
               |}
               |""".stripMargin
          )
        }
      }
    }

    Scenario("A user whose account is barred") {
      Given("a physical item from Sierra in the catalogue API")
      val itemNumber = createSierraItemNumber
      val item = createIdentifiedSierraItemWith(itemNumber)
      val work = indexedWork().items(List(item))

      And("and a user whose account is barred")
      val patronNumber = createSierraPatronNumber
      val responses = Seq(
        (
          createHoldRequest(patronNumber, itemNumber),
          HttpResponse(
            status = StatusCodes.NotFound,
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                |{
                |  "code": 132,
                |  "specificCode": 2,
                |  "httpStatus": 500,
                |  "name": "XCirc error",
                |  "description": "XCirc error : There is a problem with your library record.  Please see a librarian."
                |}
                |""".stripMargin
            )
          )
        ),
        (
          createListHoldsRequest(patronNumber),
          createListHoldsResponse(patronNumber, items = Seq())
        ),
        (
          createItemRequest(itemNumber),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              s"""
                 |{
                 |  "total": 1,
                 |  "start": 0,
                 |  "entries": [
                 |    {
                 |      "id": "$itemNumber",
                 |      "deletedDate": "2001-01-01",
                 |      "deleted": false,
                 |      "suppressed": false,
                 |      "holdCount": 0,
                 |      "fixedFields": {
                 |        "79": {"label": "LOCATION", "value": "scmac", "display": "Closed stores Arch. & MSS"},
                 |        "88": {"label": "STATUS", "value": "-", "display": "Available"},
                 |        "108": {"label": "OPACMSG", "value": "f", "display": "Online request"}
                 |      }
                 |    }
                 |  ]
                 |}
                 |""".stripMargin
            )
          )
        )
      )

      withLocalWorksIndex { index =>
        implicit val route: Route = createRoute(index, responses)
        insertIntoElasticsearch(index, work)

        When("the user requests the item")
        val response = makePostRequest(
          path = s"/users/$patronNumber/item-requests",
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

        Then("we throw an internal server error")
        response.status shouldBe StatusCodes.InternalServerError
        response.status.intValue shouldBe 500

        And("we display a generic response")
        assertIsDisplayError(
          response,
          statusCode = StatusCodes.InternalServerError
        )
      }
    }

    Scenario("A user whose account has expired") {
      withLocalWorksIndex { index =>
        // This should be quite unlikely in practice -- we're going to block users from
        // logging in when their account is expired -- but if they do get a request off,
        // we can provide a useful error message.

        Given("a physical item from Sierra in the catalogue API")
        val itemNumber = createSierraItemNumber
        val item = createIdentifiedSierraItemWith(itemNumber)
        val work = indexedWork().items(List(item))

        And("and a user whose account has expired")
        val patronNumber = createSierraPatronNumber
        val responses = Seq(
          (
            createHoldRequest(patronNumber, itemNumber),
            HttpResponse(
              status = StatusCodes.NotFound,
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                """
                  |{
                  |  "code": 132,
                  |  "specificCode": 2,
                  |  "httpStatus": 500,
                  |  "name": "XCirc error",
                  |  "description": "XCirc error : There is a problem with your library record.  Please see a librarian."
                  |}
                  |""".stripMargin
              )
            )
          ),
          (
            createListHoldsRequest(patronNumber),
            createListHoldsResponse(patronNumber, items = Seq())
          ),
          (
            createItemRequest(itemNumber),
            HttpResponse(
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                s"""
                   |{
                   |  "total": 1,
                   |  "start": 0,
                   |  "entries": [
                   |    {
                   |      "id": "$itemNumber",
                   |      "deletedDate": "2001-01-01",
                   |      "deleted": false,
                   |      "suppressed": false,
                   |      "holdCount": 0,
                   |      "fixedFields": {
                   |        "79": {"label": "LOCATION", "value": "scmac", "display": "Closed stores Arch. & MSS"},
                   |        "88": {"label": "STATUS", "value": "-", "display": "Available"},
                   |        "108": {"label": "OPACMSG", "value": "f", "display": "Online request"}
                   |      }
                   |    }
                   |  ]
                   |}
                   |""".stripMargin
              )
            )
          ),
          (
            HttpRequest(
              uri =
                s"http://sierra:1234/v5/patrons/$patronNumber?fields=expirationDate"
            ),
            HttpResponse(
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                s"""
                   |{
                   |  "id": ${patronNumber.withoutCheckDigit},
                   |  "expirationDate": "2001-01-01"
                   |}
                   |""".stripMargin
              )
            )
          )
        )

        implicit val route: Route = createRoute(index, responses)
        insertIntoElasticsearch(index, work)

        When("the user requests the item")
        val response = makePostRequest(
          path = s"/users/$patronNumber/item-requests",
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

        Then("the API returns a Forbidden response")
        response.status shouldBe StatusCodes.Forbidden
        response.status.intValue shouldBe 403

        And("the body explains the error")
        withStringEntity(response.entity) {
          assertJsonStringsAreEqual(
            _,
            s"""
               |{
               |  "type": "Error",
               |  "errorType": "http",
               |  "httpStatus": 403,
               |  "label": "Forbidden",
               |  "description": "Your account has expired, and you're no longer able to request items. To renew your account, please <a href=\\\"library@wellcomecollection.org\\\">contact the library team</a>."
               |}
               |""".stripMargin
          )
        }
      }
    }
  }

  def makePostRequest(path: String, entity: RequestEntity)(
    implicit route: Route
  ): HttpResponse = {
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

  def createIdentifiedSierraItemWith(
    itemNumber: SierraItemNumber
  ): Item[IdState.Identified] =
    createIdentifiedItemWith(
      sourceIdentifier = createSourceIdentifierWith(
        identifierType = IdentifierType.SierraSystemNumber,
        value = itemNumber.withCheckDigit,
        ontologyType = "Item"
      ),
      locations = List(createPhysicalLocation)
    )

  def createRoute(
    index: Index,
    responses: Seq[(HttpRequest, HttpResponse)] = Seq(),
    holdLimit: Int = 10
  ): Route = {
    val client = new MemoryHttpClient(responses) with HttpGet with HttpPost {
      override val baseUri: Uri = Uri("http://sierra:1234")
    }

    val requestsService = new RequestsService(
      sierraService = SierraRequestsService(client, holdLimit = holdLimit),
      itemLookup = ItemLookup(elasticClient, index = index)
    )

    val api: RequestsApi = new RequestsApi(requestsService)

    api.routes
  }

  def createListHoldsRequest(patron: SierraPatronNumber): HttpRequest =
    HttpRequest(
      method = HttpMethods.GET,
      uri = s"http://sierra:1234/v5/patrons/$patron/holds?limit=100&offset=0"
    )

  def createListHoldsResponse(
    patron: SierraPatronNumber,
    items: Seq[SierraItemNumber]
  ): HttpResponse =
    HttpResponse(
      entity = HttpEntity(
        contentType = ContentTypes.`application/json`,
        s"""
           |{
           |  "total": ${items.size},
           |  "start": 0,
           |  "entries": [
           |    ${items
             .map(it => createListHoldEntry(patron, it))
             .mkString(",")}
           |  ]
           |}
           |""".stripMargin
      )
    )

  private def createListHoldEntry(
    patron: SierraPatronNumber,
    item: SierraItemNumber
  ): String =
    s"""
       |{
       |  "id": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/holds/${randomInt(
         from = 0,
         to = 10000
       )}",
       |  "record": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/items/${item.withoutCheckDigit}",
       |  "patron": "https://libsys.wellcomelibrary.org/iii/sierra-api/v6/patrons/${patron.withoutCheckDigit}",
       |  "frozen": false,
       |  "placed": "2021-05-07",
       |  "notWantedBeforeDate": "2021-05-07",
       |  "pickupLocation": {"code":"sotop", "name":"Rare Materials Room"},
       |  "status": {"code": "0", "name": "on hold."}
       |}
       |""".stripMargin

  def createHoldRequest(
    patron: SierraPatronNumber,
    item: SierraItemNumber
  ): HttpRequest =
    HttpRequest(
      method = HttpMethods.POST,
      uri = s"http://sierra:1234/v5/patrons/$patron/holds/requests",
      entity = HttpEntity(
        contentType = ContentTypes.`application/json`,
        s"""
           |{
           |  "recordType": "i",
           |  "recordNumber": ${item.withoutCheckDigit},
           |  "pickupLocation": "unspecified"
           |}
           |""".stripMargin
      )
    )
}
