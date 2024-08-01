package weco.api.items.services

import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.items.fixtures.ItemsApiGenerators
import weco.api.stacks.models.CatalogueWork
import weco.catalogue.display_model.generators.IdentifiersGenerators
import weco.catalogue.display_model.identifiers.{
  DisplayIdentifier,
  DisplayIdentifierType
}
import weco.catalogue.display_model.work.DisplayItem
import weco.fixtures.TestWith
import weco.json.utils.JsonAssertions
import weco.sierra.fixtures.SierraSourceFixture
import weco.sierra.generators.SierraIdentifierGenerators

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ItemUpdateServiceTest
    extends AnyFunSpec
    with Matchers
    with JsonAssertions
    with ItemsApiGenerators
    with SierraSourceFixture
    with IdentifiersGenerators
    with SierraIdentifierGenerators
    with IntegrationPatience {

  def withItemUpdateService[R](
    itemUpdaters: List[ItemUpdater]
  )(testWith: TestWith[ItemUpdateService, R]): R =
    testWith(new ItemUpdateService(itemUpdaters))

  def createSierraSystemSourceIdentifier: DisplayIdentifier =
    DisplayIdentifier(
      identifierType = DisplayIdentifierType.SierraSystemNumber,
      value = randomAlphanumeric()
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
            identifiers = List(createSierraSystemSourceIdentifier),
            availableDates = None
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
              identifiers = List(createSierraSystemSourceIdentifier),
              availableDates = None
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
}

// add a case to check it returns all items, not just the PhysicalItems
// add a case to check it only passes PhysicalItems to the updater
