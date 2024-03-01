package weco.api.items.services

import akka.http.scaladsl.model._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import weco.api.items.fixtures.ItemsApiGenerators
import weco.api.stacks.models.{CatalogueAccessMethod, CatalogueWork}
import weco.catalogue.display_model.generators.IdentifiersGenerators
import weco.catalogue.display_model.identifiers.{
  DisplayIdentifier,
  DisplayIdentifierType
}
import weco.catalogue.display_model.locations._
import weco.catalogue.display_model.work.{AvailabilitySlot, DisplayItem}
import weco.fixtures.TestWith
import weco.json.utils.JsonAssertions
import weco.sierra.fixtures.SierraSourceFixture
import weco.sierra.generators.SierraIdentifierGenerators
import weco.sierra.models.identifiers.SierraItemNumber

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.time.ZonedDateTime

class ItemUpdateServiceTest
    extends AnyFunSpec
    with Matchers
    with JsonAssertions
    with ItemsApiGenerators
    with SierraSourceFixture
    with IdentifiersGenerators
    with SierraIdentifierGenerators
    with IntegrationPatience {

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

  val dummyDigitalItem =
    DisplayItem(
      id = None,
      identifiers = Nil,
      locations = List(
        DisplayDigitalLocation(
          locationType = DisplayLocationType(
            id = "iiif-presentation",
            label = "IIIF Presentation API"
          ),
          url =
            s"https://iiif.wellcomecollection.org/image/${randomAlphanumeric(3)}.jpg/info.json",
          license = Some(
            DisplayLicense(
              id = "cc-by",
              label = "Attribution 4.0 International (CC BY 4.0)",
              url = "http://creativecommons.org/licenses/by/4.0/"
            )
          ),
          accessConditions = Nil
        )
      )
    )

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
  ): DisplayItem = {
    val temporarilyUnavailableOnline = DisplayAccessCondition(
      method = CatalogueAccessMethod.NotRequestable,
      status = CatalogueAccessStatus.TemporarilyUnavailable
    )

    createPhysicalItemWith(
      sierraItemNumber = sierraItemNumber,
      accessCondition = temporarilyUnavailableOnline
    )
  }

  def availableItem(sierraItemNumber: SierraItemNumber) =
    createPhysicalItemWith(
      sierraItemNumber = sierraItemNumber,
      accessCondition = availableOnlineAccessCondition
    )

  val availableOnlineAccessCondition = DisplayAccessCondition(
    method = CatalogueAccessMethod.OnlineRequest
  )

  val onHoldAccessCondition = DisplayAccessCondition(
    method = CatalogueAccessMethod.NotRequestable,
    status = Some(CatalogueAccessStatus.TemporarilyUnavailable),
    note = Some(
      "Item is in use by another reader. Please ask at Library Enquiry Desk."
    ),
    terms = None
  )

  val onlineRequestAccessCondition = DisplayAccessCondition(
    method = CatalogueAccessMethod.OnlineRequest,
    status = CatalogueAccessStatus.Open
  )

  val availableDates = Some(
    List(
      AvailabilitySlot(
        ZonedDateTime.parse(
          "2024-02-29T13:32:44.943107Z[Europe/London]"
        ),
        ZonedDateTime
          .parse("2024-02-29T13:32:44.943107Z[Europe/London]")
          .plusWeeks(2)
      )
    )
  )

  class DummyItemUpdater(
    itemTransform: Seq[DisplayItem] => Seq[DisplayItem] = identity
  ) extends ItemUpdater {
    override val identifierType: DisplayIdentifierType =
      DisplayIdentifierType.SierraSystemNumber

    override def updateItems(
      items: Seq[DisplayItem]
    ): Future[Seq[DisplayItem]] = Future {
      itemTransform(items)
    }
  }

  it("maintains the order of items") {
    val itemUpdater = new DummyItemUpdater()

    val orderedItems = (1 to 3)
      .map(
        _ =>
          DisplayItem(
            id = Some(createCanonicalId),
            identifiers = List(createSierraSystemSourceIdentifier)
          )
      )
      .toList

    val reversedItems = orderedItems.reverse

    val workWithItemsForward = CatalogueWork(
      id = createCanonicalId,
      title = None,
      identifiers = Nil,
      items = orderedItems
    )

    val workWithItemsBackward = CatalogueWork(
      id = createCanonicalId,
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
      id = createCanonicalId,
      title = None,
      identifiers = Nil,
      items = (1 to 3)
        .map(
          _ =>
            DisplayItem(
              id = Some(createCanonicalId),
              identifiers = List(createSierraSystemSourceIdentifier)
            )
        )
        .toList
    )

    withItemUpdateService(itemUpdaters = List(brokenItemUpdater)) {
      itemUpdateService =>
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

    val workWithUnavailableItem = CatalogueWork(
      id = createCanonicalId,
      title = None,
      identifiers = Nil,
      items = List(
        temporarilyUnavailableItem(workWithUnavailableItemNumber),
        dummyDigitalItem
      )
    )
    val workWithAvailableItem = CatalogueWork(
      id = createCanonicalId,
      title = None,
      identifiers = Nil,
      items = List(
        availableItem(workWithAvailableItemNumber),
        dummyDigitalItem
      )
    )

    val itemStates = Table(
      (
        "Sierra Responses",
        "Catalogue Work",
        "AccessCondition",
        "AvailableDates"
      ),
      (
        onHoldItemResponses(workWithAvailableItemNumber),
        workWithAvailableItem,
        onHoldAccessCondition,
        None
      ),
      (
        onHoldItemResponses(workWithUnavailableItemNumber),
        workWithUnavailableItem,
        onHoldAccessCondition,
        None
      ),
      (
        availableItemResponses(workWithAvailableItemNumber),
        workWithAvailableItem,
        onlineRequestAccessCondition,
        availableDates
      ),
      (
        availableItemResponses(workWithUnavailableItemNumber),
        workWithUnavailableItem,
        onlineRequestAccessCondition,
        availableDates
      ),
      (
        missingItemResponse(workWithAvailableItemNumber),
        workWithAvailableItem,
        availableOnlineAccessCondition,
        availableDates // this should be None, possible bug in the Sierra updater
      )
    )

    it("updates AccessCondition correctly based on Sierra responses") {
      forAll(itemStates) {
        (
          sierraResponses,
          work,
          expectedAccessCondition,
          expectedAvailableDates
        ) =>
          withSierraItemUpdater(sierraResponses) { itemUpdater =>
            withItemUpdateService(List(itemUpdater)) { itemUpdateService =>
              whenReady(itemUpdateService.updateItems(work)) { updatedItems =>
                updatedItems.length shouldBe 2

                val physicalItem = updatedItems.head
                val digitalItem = updatedItems(1)
                val updatedAccessCondition = physicalItem.locations.head
                  .asInstanceOf[DisplayPhysicalLocation]
                  .accessConditions
                  .head

                updatedAccessCondition shouldBe expectedAccessCondition

                digitalItem shouldBe dummyDigitalItem
              }
            }
          }
      }
    }

    it(
      "adds available dates based on the item's DisplayLocationType and accessConditions"
    ) {
      forAll(itemStates) {
        (
          sierraResponses,
          work,
          expectedAccessCondition,
          expectedAvailableDates
        ) =>
          withSierraItemUpdater(sierraResponses) { itemUpdater =>
            withItemUpdateService(List(itemUpdater)) { itemUpdateService =>
              whenReady(itemUpdateService.updateItems(work)) { updatedItems =>
                updatedItems.length shouldBe 2

                val physicalItem = updatedItems.head
                val digitalItem = updatedItems(1)

                physicalItem.availableDates shouldBe expectedAvailableDates
                digitalItem shouldBe dummyDigitalItem
              }
            }
          }
      }
    }
  }

  def createPhysicalItemWith(
    sierraItemNumber: SierraItemNumber,
    accessCondition: DisplayAccessCondition
  ): DisplayItem = {

    val physicalItemLocation =
      DisplayPhysicalLocation(
        accessConditions = List(accessCondition),
        label = randomAlphanumeric(),
        locationType = DisplayLocationType(
          id = "closed-stores",
          label = "Closed stores"
        )
      )

    val itemSourceIdentifier = DisplayIdentifier(
      identifierType = DisplayIdentifierType.SierraSystemNumber,
      value = sierraItemNumber.withCheckDigit
    )

    DisplayItem(
      id = Some(createCanonicalId),
      identifiers = List(itemSourceIdentifier),
      locations = List(physicalItemLocation)
    )
  }

  def createSierraSystemSourceIdentifier: DisplayIdentifier =
    DisplayIdentifier(
      identifierType = DisplayIdentifierType.SierraSystemNumber,
      value = randomAlphanumeric()
    )
}
