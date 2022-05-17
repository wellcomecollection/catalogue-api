package weco.api.requests.services

import akka.http.scaladsl.model.{HttpResponse, Uri}
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.requests.fixtures.ItemLookupFixture
import weco.api.requests.models.RequestedItemWithWork
import weco.catalogue.display_model.identifiers.DisplayIdentifier
import weco.catalogue.display_model.work.DisplayItem
import weco.catalogue.internal_model.identifiers.SourceIdentifier
import weco.catalogue.internal_model.work.generators.{
  ItemsGenerators,
  WorkGenerators
}
import weco.http.client.{HttpGet, MemoryHttpClient}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ItemLookupTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with ItemsGenerators
    with WorkGenerators
    with ScalaFutures
    with ItemLookupFixture {

  describe("byCanonicalId") {
    it("finds a work with the same item ID") {
      val item1 = createIdentifiedItem
      val item2 = createIdentifiedItem
      val item3 = createIdentifiedItem

      val workSourceIds = List(
        createSourceIdentifier,
        createSourceIdentifier
      ).sortBy(_.value)

      val workA = indexedWork(workSourceIds(0)).items(List(item1, item2))
      val workB = indexedWork(workSourceIds(1)).items(List(item2, item3))

      val responses = Seq(
        (
          catalogueItemRequest(item1.id.canonicalId),
          catalogueWorkResponse(Seq(workA))
        ),
        (
          catalogueItemRequest(item2.id.canonicalId),
          catalogueWorkResponse(Seq(workA, workB))
        ),
        (
          catalogueItemRequest(item3.id.canonicalId),
          catalogueWorkResponse(Seq(workB))
        )
      )

      withItemLookup(responses) { lookup =>
        Seq(item1, item2, item3).foreach { item =>
          val future =
            lookup.byCanonicalId(item.id.canonicalId)

          whenReady(future) {
            _ shouldBe Right(DisplayIdentifier(item.id.sourceIdentifier))
          }
        }
      }
    }

    it("returns a NotFoundError if there is no such work") {
      val id = createCanonicalId

      val responses = Seq(
        (
          catalogueItemRequest(id),
          catalogueWorkResponse(Seq())
        )
      )

      val future = withItemLookup(responses) {
        _.byCanonicalId(id)
      }

      whenReady(future) {
        _.left.value shouldBe a[ItemNotFoundError]
      }
    }

    it("returns an error if the API has an error") {
      val brokenClient = new MemoryHttpClient(responses = Seq()) with HttpGet {
        override val baseUri: Uri = Uri("http://catalogue:9001")

        override def get(
          path: Uri.Path,
          params: Map[String, String]
        ): Future[HttpResponse] =
          Future.failed(new Throwable("BOOM!"))
      }

      val future = withActorSystem { implicit actorSystem =>
        val lookup = new ItemLookup(brokenClient)

        lookup.byCanonicalId(createCanonicalId)
      }

      whenReady(future) {
        _.left.value shouldBe a[UnknownItemError]
      }
    }
  }

  describe("bySourceIdentifier") {
    it("finds items by source identifier") {
      val item1 = createIdentifiedItem
      val item2 = createIdentifiedItem
      val item3 = createIdentifiedItem

      val workSourceIds = createSortedSourceIdentifiers(count = 2)

      // Enforcing ordering of source identifier value to ensure consistent
      // results when items appear on multiple works
      val workA = indexedWork(workSourceIds(0)).items(List(item1, item2))
      val workB = indexedWork(workSourceIds(1)).items(List(item2, item3))

      val responses = Seq(
        (
          catalogueItemsRequest(
            item1.id.sourceIdentifier,
            item2.id.sourceIdentifier
          ),
          catalogueWorkResponse(Seq(workA, workB))
        )
      )

      val future = withItemLookup(responses) {
        _.bySourceIdentifier(
          Seq(
            item1.id.sourceIdentifier,
            item2.id.sourceIdentifier
          )
        )
      }

      whenReady(future) {
        _ shouldBe List(
          Right(
            RequestedItemWithWork(
              workA.state.canonicalId,
              workA.data.title,
              DisplayItem(item1)
            )
          ),
          Right(
            RequestedItemWithWork(
              workA.state.canonicalId,
              workA.data.title,
              DisplayItem(item2)
            )
          )
        )
      }
    }

    it("handles a work where some items have no identifiers") {
      val item1 = createIdentifiedItem
      val item2 = createIdentifiedItem
      val item3 = createUnidentifiableItem

      val workSourceIds = createSortedSourceIdentifiers(count = 2)

      // Enforcing ordering of source identifier value to ensure consistent
      // results when items appear on multiple works
      val workA = indexedWork(workSourceIds(0)).items(List(item1, item2))
      val workB = indexedWork(workSourceIds(1)).items(List(item2, item3))

      val responses = Seq(
        (
          catalogueItemsRequest(
            item1.id.sourceIdentifier,
            item2.id.sourceIdentifier
          ),
          catalogueWorkResponse(Seq(workA, workB))
        )
      )

      val future = withItemLookup(responses) {
        _.bySourceIdentifier(
          Seq(
            item1.id.sourceIdentifier,
            item2.id.sourceIdentifier
          )
        )
      }

      whenReady(future) {
        _ shouldBe List(
          Right(
            RequestedItemWithWork(
              workA.state.canonicalId,
              workA.data.title,
              DisplayItem(item1)
            )
          ),
          Right(
            RequestedItemWithWork(
              workA.state.canonicalId,
              workA.data.title,
              DisplayItem(item2)
            )
          )
        )
      }
    }

    it("returns not found errors where ID matches cannot be found") {
      val item1 = createIdentifiedItem
      val item2 = createIdentifiedItem
      val item3 = createIdentifiedItem
      val item4 = createIdentifiedItem

      val workSourceIds = List(
        createSourceIdentifier,
        createSourceIdentifier
      ).sortBy(_.value)

      val workA = indexedWork(workSourceIds(0)).items(List(item1, item2))
      val workB = indexedWork(workSourceIds(1)).items(List(item2, item3))

      val responses = Seq(
        (
          catalogueItemsRequest(
            item1.id.sourceIdentifier,
            item4.id.sourceIdentifier,
            item3.id.sourceIdentifier
          ),
          catalogueWorkResponse(Seq(workA, workB))
        )
      )

      val future = withItemLookup(responses) {
        _.bySourceIdentifier(
          Seq(
            item1.id.sourceIdentifier,
            item4.id.sourceIdentifier,
            item3.id.sourceIdentifier
          )
        )
      }

      whenReady(future) { result =>
        result should have size 3

        result(0).value shouldBe RequestedItemWithWork(
          workA.state.canonicalId,
          workA.data.title,
          DisplayItem(item1)
        )
        result(1).left.value shouldBe a[ItemNotFoundError]
        result(2).value shouldBe RequestedItemWithWork(
          workB.state.canonicalId,
          workB.data.title,
          DisplayItem(item3)
        )
      }
    }

    it("chooses work details based on work source id ordering") {
      val item1 = createIdentifiedItem
      val item2 = createIdentifiedItem
      val item3 = createIdentifiedItem

      val workSourceIds = createSortedSourceIdentifiers(count = 2)

      val workA = indexedWork(workSourceIds(0)).items(List(item1, item2))
      val workB = indexedWork(workSourceIds(1)).items(List(item2, item3))

      val responses = Seq(
        (
          catalogueItemsRequest(item1.id.sourceIdentifier),
          catalogueWorkResponse(Seq(workA))
        ),
        (
          catalogueItemsRequest(item2.id.sourceIdentifier),
          catalogueWorkResponse(Seq(workB, workA))
        ),
        (
          catalogueItemsRequest(item3.id.sourceIdentifier),
          catalogueWorkResponse(Seq(workB))
        )
      )

      withItemLookup(responses) { lookup =>
        Seq((workA, item1), (workA, item2), (workB, item3)).foreach {
          case (work, item) =>
            val future =
              lookup.bySourceIdentifier(Seq(item.id.sourceIdentifier))

            whenReady(future) {
              _ shouldBe List(
                Right(
                  RequestedItemWithWork(
                    work.state.canonicalId,
                    work.data.title,
                    DisplayItem(item)
                  )
                )
              )
            }
        }
      }
    }

    it("does not match on anything but the first identifier") {
      val item1 = createIdentifiedItemWith(
        otherIdentifiers = List(createSourceIdentifier)
      )
      val item2 = createIdentifiedItemWith(
        otherIdentifiers = List(createSourceIdentifier)
      )
      val item3 = createIdentifiedItemWith(
        otherIdentifiers = List(createSourceIdentifier)
      )

      val workSourceIds = createSortedSourceIdentifiers(count = 2)

      val workA = indexedWork(workSourceIds(0)).items(List(item1, item2))
      val workB = indexedWork(workSourceIds(1)).items(List(item2, item3))

      val responses = Seq(
        (
          catalogueItemsRequest(item1.id.sourceIdentifier),
          catalogueWorkResponse(Seq(workA))
        ),
        (
          catalogueItemsRequest(item1.id.otherIdentifiers.head),
          catalogueWorkResponse(Seq(workA))
        ),
        (
          catalogueItemsRequest(item2.id.sourceIdentifier),
          catalogueWorkResponse(Seq(workA, workB))
        ),
        (
          catalogueItemsRequest(item2.id.otherIdentifiers.head),
          catalogueWorkResponse(Seq(workA, workB))
        ),
        (
          catalogueItemsRequest(item3.id.sourceIdentifier),
          catalogueWorkResponse(Seq(workB))
        ),
        (
          catalogueItemsRequest(item3.id.otherIdentifiers.head),
          catalogueWorkResponse(Seq(workB))
        )
      )

      withItemLookup(responses) { lookup =>
        List((workA, item1), (workA, item2), (workB, item3)).foreach {
          case (work, item) =>
            whenReady(lookup.bySourceIdentifier(List(item.id.sourceIdentifier))) {
              _ shouldBe List(
                Right(
                  RequestedItemWithWork(
                    work.state.canonicalId,
                    work.data.title,
                    DisplayItem(item)
                  )
                )
              )
            }

            whenReady(
              lookup.bySourceIdentifier(List(item.id.otherIdentifiers.head))
            ) { result =>
              result should have size 1
              result.head.left.value shouldBe a[ItemNotFoundError]
            }
        }
      }
    }

    it("returns an error if the API lookup fails") {
      val brokenClient = new MemoryHttpClient(responses = Seq()) with HttpGet {
        override val baseUri: Uri = Uri("http://catalogue:9001")

        override def get(
          path: Uri.Path,
          params: Map[String, String]
        ): Future[HttpResponse] =
          Future.failed(new Throwable("BOOM!"))
      }

      val future = withActorSystem { implicit actorSystem =>
        val lookup = new ItemLookup(brokenClient)

        lookup.bySourceIdentifier(Seq(createSourceIdentifier))
      }

      whenReady(future) { err =>
        err should have size 1
        err.head.left.value shouldBe a[UnknownItemError]
      }
    }

    it("can fetch an empty list of items") {
      val responses = Seq()

      val future = withItemLookup(responses) {
        _.bySourceIdentifier(itemIdentifiers = Seq())
      }

      whenReady(future) {
        _ shouldBe empty
      }
    }
  }

  // It seems like Elasticsearch and Scala disagree about how source identifiers should
  // be sorted, especially when the case of two identifiers is different.  This causes
  // some of the sort-related tests to be flaky.
  //
  // Because we're usually doing this with Sierra identifiers which start with a
  // lowercase 'b', just lowercase all the values we use in these tests.
  private def createSortedSourceIdentifiers(count: Int): Seq[SourceIdentifier] =
    (1 to count)
      .map { _ =>
        createSourceIdentifier
      }
      .map { s =>
        s.copy(value = s.value.toLowerCase)
      }
      .sortBy { _.value }
}
