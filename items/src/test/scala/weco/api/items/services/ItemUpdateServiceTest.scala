package weco.api.items.services

import akka.http.scaladsl.model._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import weco.api.items.fixtures.ItemsApiGenerators
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.locations.AccessStatus.TemporarilyUnavailable
import weco.catalogue.internal_model.locations.{
  AccessCondition,
  AccessMethod,
  AccessStatus
}
import weco.catalogue.internal_model.work.Item
import weco.json.utils.JsonAssertions

class ItemUpdateServiceTest
    extends AnyFunSpec
    with Matchers
    with JsonAssertions
    with ScalaFutures
    with ItemsApiGenerators {

  val sierraItemNumber = createSierraItemNumber
  val dummyDigitalItem = createDigitalItem

  val availableItemResponses = Seq(
    (
      HttpRequest(uri = sierraUri(sierraItemNumber)),
      HttpResponse(
        entity = sierraItemResponse(
          sierraItemNumber = sierraItemNumber
        )
      )
    )
  )

  val onHoldItemResponses = Seq(
    (
      HttpRequest(uri = sierraUri(sierraItemNumber)),
      HttpResponse(
        entity = sierraItemResponse(
          sierraItemNumber = sierraItemNumber,
          holdCount = 1
        )
      )
    )
  )

  val temporarilyUnavailableItem: Item[IdState.Identified] = {
    val temporarilyUnavailableOnline = AccessCondition(
      method = AccessMethod.OnlineRequest,
      status = AccessStatus.TemporarilyUnavailable
    )

    createPhysicalItemWith(
      sierraItemNumber = sierraItemNumber,
      accessCondition = temporarilyUnavailableOnline
    )
  }

  val availableItem = {
    val availableOnline = AccessCondition(
      method = AccessMethod.OnlineRequest
    )

    createPhysicalItemWith(
      sierraItemNumber = sierraItemNumber,
      accessCondition = availableOnline
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

  val workWithUnavailableItem = indexedWork().items(
    List(
      temporarilyUnavailableItem,
      dummyDigitalItem
    )
  )

  val workWithAvailableItem = indexedWork().items(
    List(
      availableItem,
      dummyDigitalItem
    )
  )

  val itemStates = Table(
    ("Sierra Responses", "Catalogue Work", "AccessCondition"),
    (
      onHoldItemResponses,
      workWithAvailableItem,
      onHoldAccessCondition
    ),
    (
      onHoldItemResponses,
      workWithUnavailableItem,
      onHoldAccessCondition
    ),
    (
      availableItemResponses,
      workWithAvailableItem,
      onlineRequestAccessCondition
    ),
    (
      availableItemResponses,
      workWithUnavailableItem,
      onlineRequestAccessCondition
    )
  )

  it("updates AccessCondition correctly based on Sierra responses") {
    forAll(itemStates) {
      (sierraResponses, catalogueWork, expectedAccessCondition) =>
        withItemUpdateService(sierraResponses) { itemUpdateService =>
          whenReady(itemUpdateService.updateItems(catalogueWork)) {
            updatedItems =>
              updatedItems.length shouldBe 2

              val physicalItem = updatedItems(0)
              val digitalItem = updatedItems(1)

              physicalItem.locations.head.accessConditions.head shouldBe expectedAccessCondition
              digitalItem shouldBe dummyDigitalItem
          }
        }
    }
  }
}
