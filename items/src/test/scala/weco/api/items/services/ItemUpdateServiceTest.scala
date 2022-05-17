package weco.api.items.services

import akka.http.scaladsl.model._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import weco.api.items.fixtures.ItemsApiGenerators
import weco.api.stacks.models.CatalogueWork
import weco.catalogue.display_model.identifiers.DisplayIdentifier
import weco.catalogue.display_model.locations.{
  DisplayAccessCondition,
  DisplayPhysicalLocation
}
import weco.catalogue.display_model.work.DisplayItem
import weco.catalogue.internal_model.identifiers.{IdState, IdentifierType}
import weco.catalogue.internal_model.locations.AccessStatus.TemporarilyUnavailable
import weco.catalogue.internal_model.locations.{
  AccessCondition,
  AccessMethod,
  AccessStatus
}
import weco.catalogue.internal_model.work.Item
import weco.catalogue.internal_model.work.generators.WorkGenerators
import weco.fixtures.TestWith
import weco.json.utils.JsonAssertions
import weco.sierra.fixtures.SierraSourceFixture
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
    with WorkGenerators
    with SierraSourceFixture {

  def withSierraItemUpdater[R](
    responses: Seq[(HttpRequest, HttpResponse)] = Seq()
  )(testWith: TestWith[ItemUpdater, R]): R =
    withSierraSource(responses) { sierraSource =>
      testWith(new SierraItemUpdater(sierraSource))
    }

  def withItemUpdateService[R](
    itemUpdaters: List[ItemUpdater]
  )(testWith: TestWith[ItemUpdateService, R]): R =
    testWith(new ItemUpdateService(itemUpdaters))

  val dummyDigitalItem = createDigitalItem

  def missingItemResponse(sierraItemNumber: SierraItemNumber) = Seq(
    (
      sierraItemRequest(sierraItemNumber),
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
    note = Some(
      "Item is in use by another reader. Please ask at Library Enquiry Desk."
    )
  )

  val onlineRequestAccessCondition = AccessCondition(
    method = AccessMethod.OnlineRequest,
    status = AccessStatus.Open
  )

  val notRequestableAccessCondition = AccessCondition(
    method = AccessMethod.NotRequestable
  )

  class DummyItemUpdater(
    itemTransform: Seq[DisplayItem] => Seq[DisplayItem] = identity
  ) extends ItemUpdater {
    override val identifierType: IdentifierType =
      IdentifierType.SierraSystemNumber

    override def updateItems(
      items: Seq[DisplayItem]
    ): Future[Seq[DisplayItem]] = Future {
      itemTransform(items)
    }
  }

  def randomCanonicalId: String =
    randomAlphanumeric()

  it("maintains the order of items") {
    val itemUpdater = new DummyItemUpdater()

    val orderedItems = List(
      DisplayItem(
        id = Some(randomCanonicalId),
        identifiers = List(DisplayIdentifier(createSierraSystemSourceIdentifier))
      ),
      DisplayItem(
        id = Some(randomCanonicalId),
        identifiers = List(DisplayIdentifier(createSierraSystemSourceIdentifier))
      ),
      DisplayItem(
        id = Some(randomCanonicalId),
        identifiers = List(DisplayIdentifier(createSierraSystemSourceIdentifier))
      ),
    )

    val reversedItems = orderedItems.reverse

    val workWithItemsForward = CatalogueWork(
      id = randomCanonicalId,
      title = None,
      identifiers = Nil,
      items = orderedItems
    )

    val workWithItemsBackward = CatalogueWork(
      id = randomCanonicalId,
      title = None,
      identifiers = Nil,
      items = orderedItems.reverse
    )

    withItemUpdateService(List(itemUpdater)) { itemUpdateService =>
      whenReady(itemUpdateService.updateItems(workWithItemsForward)) {
        _ shouldBe orderedItems
      }

      whenReady(itemUpdateService.updateItems(workWithItemsBackward)) {
        _ shouldBe reversedItems
      }
    }
  }

  it("detects if the item updater returns items with differing IDs") {
    def badUpdate(items: Seq[DisplayItem]) = items.tail

    val brokenItemUpdater = new DummyItemUpdater(badUpdate)

    val workWithItems = CatalogueWork(
      id = randomCanonicalId,
      title = None,
      identifiers = Nil,
      items = List(
        DisplayItem(
          id = Some(randomCanonicalId),
          identifiers = List(DisplayIdentifier(createSierraSystemSourceIdentifier))
        ),
        DisplayItem(
          id = Some(randomCanonicalId),
          identifiers = List(DisplayIdentifier(createSierraSystemSourceIdentifier))
        ),
        DisplayItem(
          id = Some(randomCanonicalId),
          identifiers = List(DisplayIdentifier(createSierraSystemSourceIdentifier))
        ),
      )
    )

    withItemUpdateService(itemUpdaters = List(brokenItemUpdater)) { itemUpdateService =>
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
      ),
      (
        missingItemResponse(workWithAvailableItemNumber),
        workWithAvailableItem,
        notRequestableAccessCondition
      )
    )

    it("updates AccessCondition correctly based on Sierra responses") {
      forAll(itemStates) {
        (sierraResponses, catalogueWork, expectedAccessCondition) =>
          withSierraItemUpdater(sierraResponses) { itemUpdater =>
            withItemUpdateService(List(itemUpdater)) { itemUpdateService =>
              val work = CatalogueWork(catalogueWork)

              whenReady(itemUpdateService.updateItems(work)) { updatedItems =>
                updatedItems.length shouldBe 2

                val physicalItem = updatedItems(0)
                val digitalItem = updatedItems(1)

                val updatedAccessCondition = physicalItem.locations.head
                  .asInstanceOf[DisplayPhysicalLocation]
                  .accessConditions
                  .head
                updatedAccessCondition shouldBe DisplayAccessCondition(
                  expectedAccessCondition
                )

                digitalItem shouldBe DisplayItem(dummyDigitalItem)
              }
            }
          }
      }
    }
  }
}
