package weco.api.items.services

import akka.http.scaladsl.model._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import weco.api.items.fixtures.ItemsApiGenerators
import weco.api.stacks.models.{CatalogueAccessMethod, CatalogueWork}
import weco.catalogue.display_model.identifiers.{
  DisplayIdentifier,
  DisplayIdentifierType
}
import weco.catalogue.display_model.locations._
import weco.catalogue.display_model.work.DisplayItem
import weco.catalogue.internal_model.identifiers.IdentifierType
import weco.fixtures.{RandomGenerators, TestWith}
import weco.json.utils.JsonAssertions
import weco.sierra.fixtures.SierraSourceFixture
import weco.sierra.generators.SierraIdentifierGenerators
import weco.sierra.models.identifiers.SierraItemNumber

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ItemUpdateServiceTest
    extends AnyFunSpec
    with Matchers
    with JsonAssertions
    with ItemsApiGenerators
    with RandomGenerators
    with SierraSourceFixture
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

  def availableItem(sierraItemNumber: SierraItemNumber) = {
    val availableOnline = DisplayAccessCondition(
      method = CatalogueAccessMethod.OnlineRequest
    )

    createPhysicalItemWith(
      sierraItemNumber = sierraItemNumber,
      accessCondition = availableOnline
    )
  }

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

  val notRequestableAccessCondition = DisplayAccessCondition(
    method = CatalogueAccessMethod.NotRequestable
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

    val orderedItems = (1 to 3)
      .map(
        _ =>
          DisplayItem(
            id = Some(randomCanonicalId),
            identifiers = List(createSierraSystemSourceIdentifier)
          )
      )
      .toList

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
      items = (1 to 3)
        .map(
          _ =>
            DisplayItem(
              id = Some(randomCanonicalId),
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
      id = randomCanonicalId,
      title = None,
      identifiers = Nil,
      items = List(
        temporarilyUnavailableItem(workWithUnavailableItemNumber),
        dummyDigitalItem
      )
    )

    val workWithAvailableItem = CatalogueWork(
      id = randomCanonicalId,
      title = None,
      identifiers = Nil,
      items = List(
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
      forAll(itemStates) { (sierraResponses, work, expectedAccessCondition) =>
        withSierraItemUpdater(sierraResponses) { itemUpdater =>
          withItemUpdateService(List(itemUpdater)) { itemUpdateService =>
            whenReady(itemUpdateService.updateItems(work)) { updatedItems =>
              updatedItems.length shouldBe 2

              val physicalItem = updatedItems(0)
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
      id = Some(randomAlphanumeric(length = 8)),
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
