package weco.api.items.services

import akka.http.scaladsl.model._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import weco.api.items.fixtures.ItemsApiGenerators
import weco.catalogue.internal_model.identifiers.{IdState, IdentifierType}
import weco.catalogue.internal_model.locations.AccessStatus.TemporarilyUnavailable
import weco.catalogue.internal_model.locations.{
  AccessCondition,
  AccessMethod,
  AccessStatus
}
import weco.catalogue.internal_model.work.Item
import weco.catalogue.source_model.generators.SierraGenerators
import weco.catalogue.source_model.sierra.identifiers.SierraItemNumber
import weco.json.utils.JsonAssertions

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ItemUpdateServiceTest
    extends AnyFunSpec
    with Matchers
    with JsonAssertions
    with ScalaFutures
    with ItemsApiGenerators
    with IntegrationPatience
    with SierraGenerators {

  val dummyDigitalItem = createDigitalItem

  def availableItemResponses(sierraItemNumber: SierraItemNumber) = Seq(
    (
      HttpRequest(uri = sierraUri(sierraItemNumber)),
      HttpResponse(
        entity = sierraItemResponse(
          sierraItemNumber = sierraItemNumber
        )
      )
    )
  )

  def onHoldItemResponses(sierraItemNumber: SierraItemNumber) = Seq(
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

  def temporarilyUnavailableItem(
    sierraItemNumber: SierraItemNumber
  ): Item[IdState.Identified] = {
    val temporarilyUnavailableOnline = AccessCondition(
      method = AccessMethod.OnlineRequest,
      status = AccessStatus.TemporarilyUnavailable
    )

    createPhysicalItemWith(
      sierraItemNumber = sierraItemNumber,
      accessCondition = temporarilyUnavailableOnline
    )
  }

  def availableItem(sierraItemNumber: SierraItemNumber) = {
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

  class DummyItemUpdater(
    itemTransform: Seq[Item[IdState.Identified]] => Seq[
      Item[IdState.Identified]
    ] = identity
  ) extends ItemUpdater {
    override val identifierType: IdentifierType =
      IdentifierType.SierraSystemNumber

    override def updateItems(
      items: Seq[Item[IdState.Identified]]
    ): Future[Seq[Item[IdState.Identified]]] = Future {
      itemTransform(items)
    }
  }

  it("maintains the order of items") {
    val itemUpdater = new DummyItemUpdater()

    val orderedItems = List(
      temporarilyUnavailableItem(createSierraItemNumber),
      createDigitalItem,
      availableItem(createSierraItemNumber),
      createDigitalItem,
      availableItem(createSierraItemNumber),
      temporarilyUnavailableItem(createSierraItemNumber),
      createDigitalItem,
      createDigitalItem
    )

    val reversedItems = orderedItems.reverse

    val workWithItemsForward = indexedWork().items(orderedItems)
    val workWithItemsBackward = indexedWork().items(reversedItems)

    withItemUpdateService(List(itemUpdater)) { itemUpdateService =>
      whenReady(itemUpdateService.updateItems(workWithItemsForward)) { items =>
        items shouldBe orderedItems
      }

      whenReady(itemUpdateService.updateItems(workWithItemsBackward)) { items =>
        items shouldBe reversedItems
      }
    }
  }

  describe("with SierraItemUpdater") {
    val workWithUnavailableItemNumber = createSierraItemNumber
    val workWithAvailableItemNumber = createSierraItemNumber

    val workWithUnavailableItem = indexedWork().items(
      List(
        temporarilyUnavailableItem(workWithUnavailableItemNumber),
        dummyDigitalItem
      )
    )

    val workWithAvailableItem = indexedWork().items(
      List(
        availableItem(workWithAvailableItemNumber),
        dummyDigitalItem
      )
    )

    val itemStates = Table(
      ("Sierra Responses", "Catalogue Work", "AccessCondition"),
      (
        onHoldItemResponses(workWithAvailableItemNumber),
        workWithAvailableItem,
        onHoldAccessCondition
      ),
      (
        onHoldItemResponses(workWithUnavailableItemNumber),
        workWithUnavailableItem,
        onHoldAccessCondition
      ),
      (
        availableItemResponses(workWithAvailableItemNumber),
        workWithAvailableItem,
        onlineRequestAccessCondition
      ),
      (
        availableItemResponses(workWithUnavailableItemNumber),
        workWithUnavailableItem,
        onlineRequestAccessCondition
      )
    )

    it("updates AccessCondition correctly based on Sierra responses") {
      forAll(itemStates) {
        (sierraResponses, catalogueWork, expectedAccessCondition) =>
          withSierraItemUpdater(sierraResponses) { itemUpdater =>
            withItemUpdateService(List(itemUpdater)) { itemUpdateService =>
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
  }
}
