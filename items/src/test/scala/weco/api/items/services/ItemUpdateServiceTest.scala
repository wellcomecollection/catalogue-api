package weco.api.items.services

import akka.http.scaladsl.model._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.items.fixtures.{ItemsApiGenerators, SierraServiceFixture}
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.locations.AccessStatus.TemporarilyUnavailable
import weco.catalogue.internal_model.locations.{AccessCondition, AccessMethod, AccessStatus}
import weco.catalogue.internal_model.work.{Item, Work, WorkState}
import weco.catalogue.source_model.sierra.identifiers.SierraItemNumber
import weco.fixtures.TestWith
import weco.json.utils.JsonAssertions

import scala.concurrent.ExecutionContext.Implicits.global


class ItemUpdateServiceTest extends AnyFunSpec
  with SierraServiceFixture
  with Matchers
  with JsonAssertions
  with ScalaFutures
  with ItemsApiGenerators {

  def availableItemResponses(sierraItemNumber: SierraItemNumber) = Seq(
    (
      HttpRequest(
        uri = Uri(
          f"http://sierra:1234/v5/items/${sierraItemNumber.withoutCheckDigit}?fields=deleted,fixedFields,holdCount,suppressed")
      ),
      HttpResponse(
        entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          f"""
             |{
             |  "id": "${sierraItemNumber.withCheckDigit}",
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

  def onHoldItemResponses(sierraItemNumber: SierraItemNumber) = Seq(
    (
      HttpRequest(
        uri = Uri(
          f"http://sierra:1234/v5/items/${sierraItemNumber.withoutCheckDigit}?fields=deleted,fixedFields,holdCount,suppressed")
      ),
      HttpResponse(
        entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          f"""
             |{
             |  "id": "${sierraItemNumber.withCheckDigit}",
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

  val sierraItemNumber = createSierraItemNumber

  def temporarilyUnavailableItem: Item[IdState.Identified] = {
    val temporarilyUnavailableOnline = AccessCondition(
      method = AccessMethod.OnlineRequest,
      status = AccessStatus.TemporarilyUnavailable
    )

    createPhysicalItemWith(
      sierraItemNumber = sierraItemNumber,
      accessCondition = temporarilyUnavailableOnline
    )
  }

  def availableItem = {
    val temporarilyUnavailableOnline = AccessCondition(
      method = AccessMethod.OnlineRequest,
      status = AccessStatus.TemporarilyUnavailable
    )

    createPhysicalItemWith(
      sierraItemNumber = sierraItemNumber,
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

  val workWithUnavailableItem = indexedWork().items(List(
    temporarilyUnavailableItem
  ))

  val workWithAvailableItem = indexedWork().items(List(
    availableItem
  ))

  describe("when the catalogue thinks an item is NOT available") {
    describe("but sierra knows the item is available") {
      it("the item will be marked as available") {
        runTest(
          sierraResponses = availableItemResponses(sierraItemNumber),
          catalogueWork = workWithUnavailableItem,
          expectedAccessCondition = onlineRequestAccessCondition
        )
      }
    }

    describe("and sierra agrees the item is on hold") {
      it("the item will be marked as NOT available") {
        runTest(
          sierraResponses = onHoldItemResponses(sierraItemNumber),
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
          sierraResponses = onHoldItemResponses(sierraItemNumber),
          catalogueWork = workWithAvailableItem,
          expectedAccessCondition = onHoldAccessCondition
        )
      }
    }

    describe("but sierra knows the item is available") {
      it("the item will be marked as available") {
        runTest(
          sierraResponses = availableItemResponses(sierraItemNumber),
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
