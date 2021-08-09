package weco.api.items.services

import akka.http.scaladsl.model._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import weco.api.items.fixtures.ItemsApiGenerators
import weco.api.stacks.fixtures.SierraServiceFixture
import weco.catalogue.internal_model.identifiers.{IdState, IdentifierType}
import weco.catalogue.internal_model.locations.AccessStatus.TemporarilyUnavailable
import weco.catalogue.internal_model.locations.{
  AccessCondition,
  AccessMethod,
  AccessStatus
}
import weco.catalogue.internal_model.work.Item
import weco.fixtures.TestWith
import weco.json.utils.JsonAssertions
import weco.sierra.generators.SierraIdentifierGenerators
import weco.sierra.models.identifiers.SierraItemNumber

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ItemUpdateServiceTest
    extends AnyFunSpec
    with Matchers
    with JsonAssertions
    with ScalaFutures
    with ItemsApiGenerators
    with IntegrationPatience
    with SierraIdentifierGenerators
    with SierraServiceFixture {

  def withSierraItemUpdater[R](
    responses: Seq[(HttpRequest, HttpResponse)] = Seq()
  )(testWith: TestWith[ItemUpdater, R]): R =
    withSierraService(responses) { sierraService =>
      testWith(new SierraItemUpdater(sierraService))
    }

  def withItemUpdateService[R](
    itemUpdaters: List[ItemUpdater]
  )(testWith: TestWith[ItemUpdateService, R]): R =
    testWith(new ItemUpdateService(itemUpdaters))

  val dummyDigitalItem = createDigitalItem

  def availableItemResponses(sierraItemNumber: SierraItemNumber) = Seq(
    (
      sierraItemRequest(sierraItemNumber),
      HttpResponse(
        entity = sierraItemResponse(
          sierraItemNumber = sierraItemNumber
        )
      )
    )
  )

  def onHoldItemResponses(sierraItemNumber: SierraItemNumber) = Seq(
    (
      sierraItemRequest(sierraItemNumber),
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
      method = AccessMethod.NotRequestable,
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
    method = AccessMethod.NotRequestable,
    status = Some(TemporarilyUnavailable),
    note = Some("Item is in use by another reader. Please ask at Enquiry Desk.")
  )

  val onlineRequestAccessCondition = AccessCondition(
    method = AccessMethod.OnlineRequest,
    status = AccessStatus.Open
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

  it("detects if the item updater returns items with differing IDs") {
    def badUpdate(items: Seq[Item[IdState.Identified]]) = items.tail

    val itemUpdater = new DummyItemUpdater(badUpdate)

    val startingItems = List(
      temporarilyUnavailableItem(createSierraItemNumber),
      availableItem(createSierraItemNumber),
      temporarilyUnavailableItem(createSierraItemNumber),
      createDigitalItem,
      createDigitalItem
    )

    val workWithItems = indexedWork().items(startingItems)

    withItemUpdateService(List(itemUpdater)) { itemUpdateService =>
      whenReady(itemUpdateService.updateItems(workWithItems).failed) {
        failure =>
          failure shouldBe a[IllegalArgumentException]
          failure.getMessage should include(
            "Inconsistent results updating items"
          )
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
