package weco.api.items.services

import akka.http.scaladsl.model._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.items.fixtures.{Generators, SierraServiceFixture}
import weco.catalogue.internal_model.locations.AccessStatus.TemporarilyUnavailable
import weco.catalogue.internal_model.locations.{AccessCondition, AccessMethod, AccessStatus}
import weco.catalogue.internal_model.work.{Work, WorkState}
import weco.fixtures.TestWith
import weco.json.utils.JsonAssertions

import scala.concurrent.ExecutionContext.Implicits.global


class ItemUpdateServiceTest extends AnyFunSpec
  with SierraServiceFixture
  with Matchers
  with JsonAssertions
  with ScalaFutures
  with Generators {

  def availableItemResponses(itemIdentifier: String) = Seq(
    (
      HttpRequest(
        uri = Uri(
          f"http://sierra:1234/v5/items/${itemIdentifier}?fields=deleted,fixedFields,holdCount,suppressed")
      ),
      HttpResponse(
        entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          f"""
             |{
             |  "id": "${itemIdentifier}",
             |  "deleted": false,
             |  "suppressed": false,
             |  "fixedFields": {
             |    "79": {"label": "LOCATION", "value": "scmwf", "display": "Closed stores A&MSS Well.Found."},
             |    "88": {"label": "STATUS", "value": "-", "display": "Available"},
             |    "108": {"label": "OPACMSG", "value": "f", "display": "Online request"}
             |  },
             |  "holdCount": 0
             |}
             |""".stripMargin
        )
      )
    )
  )

  def onHoldItemResponses(itemIdentifier: String) = Seq(
    (
      HttpRequest(
        uri = Uri(
          f"http://sierra:1234/v5/items/${itemIdentifier}?fields=deleted,fixedFields,holdCount,suppressed")
      ),
      HttpResponse(
        entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          f"""
             |{
             |  "id": "${itemIdentifier}",
             |  "deleted": false,
             |  "suppressed": false,
             |  "fixedFields": {
             |    "79": {"label": "LOCATION", "value": "scmwf", "display": "Closed stores A&MSS Well.Found."},
             |    "88": {"label": "STATUS", "value": "-", "display": "Available"},
             |    "108": {"label": "OPACMSG", "value": "f", "display": "Online request"}
             |  },
             |  "holdCount": 1
             |}
             |""".stripMargin
        )
      )
    )
  )

  def temporarilyUnavailableItem(itemIdentifier: String) = {
    val temporarilyUnavailableOnline = AccessCondition(
      method = AccessMethod.OnlineRequest,
      status = AccessStatus.TemporarilyUnavailable
    )

    createPhysicalItemWith(
      sierraItemIdentifier = itemIdentifier,
      accessCondition = temporarilyUnavailableOnline
    )
  }

  def availableItem(itemIdentifier: String) = {
    val temporarilyUnavailableOnline = AccessCondition(
      method = AccessMethod.OnlineRequest,
      status = AccessStatus.TemporarilyUnavailable
    )

    createPhysicalItemWith(
      sierraItemIdentifier = itemIdentifier,
      accessCondition = temporarilyUnavailableOnline
    )
  }

  val onHoldAccessCondition = AccessCondition(
    method = AccessMethod.ManualRequest,
    status = Some(TemporarilyUnavailable),
    note = Some("Item is in use by another reader. Please ask at Enquiry Desk.")
  )

  val onlineRequestAccessCondition = AccessCondition(
    method = AccessMethod.OnlineRequest
  )

  val itemIdentifier = "1823449"

  val workWithUnavailableItem = indexedWork().items(List(
    temporarilyUnavailableItem(itemIdentifier)
  ))

  val workWithAvailableItem = indexedWork().items(List(
    availableItem(itemIdentifier)
  ))

  describe("when the catalogue thinks an item is NOT available") {
    describe("but sierra knows the item is available") {
      it("the item will be marked as available") {
        runTest(
          sierraResponses = availableItemResponses(itemIdentifier),
          catalogueWork = workWithUnavailableItem,
          expectedAccessCondition = onlineRequestAccessCondition
        )
      }
    }

    describe("and sierra agrees the item is on hold") {
      it("the item will be marked as NOT available") {
        runTest(
          sierraResponses = onHoldItemResponses(itemIdentifier),
          catalogueWork = workWithUnavailableItem,
          expectedAccessCondition = onHoldAccessCondition
        )
      }
    }
  }

  describe("when the catalogue thinks an item is available") {
    describe("but sierra knows the item is NOT available") {
      it("the item will be marked as NOT available") {
        runTest(
          sierraResponses = onHoldItemResponses(itemIdentifier),
          catalogueWork = workWithAvailableItem,
          expectedAccessCondition = onHoldAccessCondition
        )
      }
    }

    describe("but sierra knows the item is available") {
      it("the item will be marked as available") {
        runTest(
          sierraResponses = availableItemResponses(itemIdentifier),
          catalogueWork = workWithAvailableItem,
          expectedAccessCondition = onlineRequestAccessCondition
        )
      }
    }
  }

  def runTest(
               sierraResponses: Seq[(HttpRequest, HttpResponse)],
               catalogueWork: Work.Visible[WorkState.Indexed],
               expectedAccessCondition: AccessCondition
             ) = {
    withItemUpdateService(sierraResponses) { itemUpdateService =>
      whenReady(itemUpdateService.updateItems(catalogueWork)) { updatedItems =>
        updatedItems.head.locations.head.accessConditions.head shouldBe expectedAccessCondition
      }
    }
  }

  def withItemUpdateService[R](
                                responses: Seq[(HttpRequest, HttpResponse)] = Seq()
                              )(testWith: TestWith[ItemUpdateService, R]): R = {
    withMaterializer { implicit mat =>
      withSierraService(responses) { sierraService =>
        testWith(new ItemUpdateService(sierraService))
      }
    }
  }
}
