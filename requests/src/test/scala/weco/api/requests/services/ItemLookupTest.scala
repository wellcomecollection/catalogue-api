package weco.api.requests.services

import com.sksamuel.elastic4s.Index
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.requests.models.RequestedItemWithWork
import weco.api.search.elasticsearch.{DocumentNotFoundError, IndexNotFoundError}
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.index.IndexFixtures
import weco.catalogue.internal_model.work.Item
import weco.catalogue.internal_model.work.generators.{
  ItemsGenerators,
  WorkGenerators
}

import scala.concurrent.ExecutionContext.Implicits.global

class ItemLookupTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with IndexFixtures
    with ItemsGenerators
    with WorkGenerators {

  def createLookup(index: Index): ItemLookup =
    ItemLookup(elasticClient, index = index)

  describe("byCanonicalId") {
    it("finds a work with the same item ID") {
      val item1: Item[IdState.Identified] = createIdentifiedItem
      val item2 = createIdentifiedItem
      val item3 = createIdentifiedItem

      val workSourceIds = List(
        createSourceIdentifier,
        createSourceIdentifier
      ).sortBy(_.value)

      val workA = indexedWork(workSourceIds(0)).items(List(item1, item2))
      val workB = indexedWork(workSourceIds(1)).items(List(item2, item3))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, workA, workB)

        val lookup = createLookup(index)

        Seq(item1, item2, item3).foreach { item =>
          val future =
            lookup.byCanonicalId(item.id.canonicalId)

          whenReady(future) {
            _ shouldBe Right(item)
          }
        }
      }
    }

    it("returns a DocumentNotFoundError if there is no such work") {
      withLocalWorksIndex { index =>
        val id = createCanonicalId

        val lookup = createLookup(index)
        val future = lookup.byCanonicalId(id)

        whenReady(future) {
          _ shouldBe Left(DocumentNotFoundError(id))
        }
      }
    }

    it("returns None if there is no visible item") {
      val item = createIdentifiedItem

      val workInvisible = indexedWork().items(List(item)).invisible()
      val workVisible = indexedWork().items(List(item))

      withLocalWorksIndex { index =>
        // First we index just the invisible work, and check we don't
        // get any results.
        insertIntoElasticsearch(index, workInvisible)

        val lookup = createLookup(index)

        val future1 = lookup.byCanonicalId(item.id.canonicalId)

        whenReady(future1) {
          _ shouldBe Left(DocumentNotFoundError(item.id.canonicalId))
        }

        // Then we index both works and run the same query, so we know the
        // invisibility of the first work was the reason it was hidden.
        insertIntoElasticsearch(index, workInvisible, workVisible)

        val future2 = lookup.byCanonicalId(item.id.canonicalId)

        whenReady(future2) {
          _ shouldBe Right(item)
        }
      }
    }

    it("returns Left[Error] if Elasticsearch has an error") {
      val lookup = createLookup(index = createIndex)
      val future = lookup.byCanonicalId(createCanonicalId)

      whenReady(future) {
        _.left.value shouldBe a[IndexNotFoundError]
      }
    }
  }

  describe("bySourceIdentifier") {
    it("finds items by source identifier") {
      val item1 = createIdentifiedItem
      val item2 = createIdentifiedItem
      val item3 = createIdentifiedItem

      val workSourceIds = List(
        createSourceIdentifier,
        createSourceIdentifier
      ).sortBy(_.value)

      // Enforcing ordering of source identifier value to ensure consistent
      // results when items appear on multiple works
      val workA = indexedWork(workSourceIds(0)).items(List(item1, item2))
      val workB = indexedWork(workSourceIds(1)).items(List(item2, item3))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, workA, workB)

        val lookup = createLookup(index)

        val future =
          lookup.bySourceIdentifier(
            Seq(
              item1.id.sourceIdentifier,
              item2.id.sourceIdentifier
            )
          )

        whenReady(future) {
          _ shouldBe List(
            Right(
              RequestedItemWithWork(
                workA.state.canonicalId,
                workA.data.title,
                item1
              )
            ),
            Right(
              RequestedItemWithWork(
                workA.state.canonicalId,
                workA.data.title,
                item2
              )
            )
          )
        }
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

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, workA, workB)

        val lookup = createLookup(index)

        val future =
          lookup.bySourceIdentifier(
            Seq(
              item1.id.sourceIdentifier,
              item4.id.sourceIdentifier,
              item3.id.sourceIdentifier
            )
          )

        whenReady(future) {
          _ shouldBe List(
            Right(
              RequestedItemWithWork(
                workA.state.canonicalId,
                workA.data.title,
                item1
              )
            ),
            Left(DocumentNotFoundError(item4.id.sourceIdentifier)),
            Right(
              RequestedItemWithWork(
                workB.state.canonicalId,
                workB.data.title,
                item3
              )
            )
          )
        }
      }
    }

    it("chooses work details based on work source id ordering") {
      val item1 = createIdentifiedItem
      val item2 = createIdentifiedItem
      val item3 = createIdentifiedItem

      val workSourceIds = List(
        createSierraSystemSourceIdentifier,
        createSierraSystemSourceIdentifier
      ).sortBy(_.value)

      val workA = indexedWork(workSourceIds(0)).items(List(item1, item2))
      val workB = indexedWork(workSourceIds(1)).items(List(item2, item3))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, workA, workB)

        val lookup = createLookup(index)

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
                    item
                  )
                )
              )
            }
        }
      }
    }

    it("does not match on identifiers in the otherIdentifiers field") {
      val item1 = createIdentifiedItemWith(
        otherIdentifiers = List(createSourceIdentifier)
      )
      val item2 = createIdentifiedItemWith(
        otherIdentifiers = List(createSourceIdentifier)
      )
      val item3 = createIdentifiedItemWith(
        otherIdentifiers = List(createSourceIdentifier)
      )

      val workSourceIds = List(
        createSierraSystemSourceIdentifier,
        createSierraSystemSourceIdentifier
      ).sortBy(_.value)

      val workA = indexedWork(workSourceIds(0)).items(List(item1, item2))
      val workB = indexedWork(workSourceIds(1)).items(List(item2, item3))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, workA, workB)

        val lookup = createLookup(index)

        List((workA, item1), (workA, item2), (workB, item3)).foreach {
          case (work, item) =>
            whenReady(lookup.bySourceIdentifier(List(item.id.sourceIdentifier))) {
              _ shouldBe List(
                Right(
                  RequestedItemWithWork(
                    work.state.canonicalId,
                    work.data.title,
                    item
                  )
                )
              )
            }

            whenReady(
              lookup.bySourceIdentifier(List(item.id.otherIdentifiers.head))
            ) {
              _ shouldBe List(
                Left(DocumentNotFoundError(item.id.otherIdentifiers.head))
              )
            }
        }
      }
    }

    it(
      "returns a DocumentNotFoundError if there is no visible work on an item"
    ) {
      val item = createIdentifiedItem

      val workInvisible = indexedWork().items(List(item)).invisible()
      val workVisible = indexedWork().items(List(item))

      withLocalWorksIndex { index =>
        // First we index just the invisible work, and check we don't
        // get any results.
        insertIntoElasticsearch(index, workInvisible)

        val lookup = createLookup(index)

        val future1 = lookup.bySourceIdentifier(Seq(item.id.sourceIdentifier))

        whenReady(future1) {
          _ shouldBe Seq(Left(DocumentNotFoundError(item.id.sourceIdentifier)))
        }

        // Then we index both works and run the same query, so we know the
        // invisibility of the first work was the reason it was hidden.
        insertIntoElasticsearch(index, workInvisible, workVisible)

        val future2 = lookup.bySourceIdentifier(Seq(item.id.sourceIdentifier))

        whenReady(future2) {
          _ shouldBe List(
            Right(
              RequestedItemWithWork(
                workVisible.state.canonicalId,
                workVisible.data.title,
                item
              )
            )
          )
        }
      }
    }

    it("returns Left[Error] if Elasticsearch has an error") {
      val lookup = createLookup(index = createIndex)
      val future =
        lookup.bySourceIdentifier(Seq(createSourceIdentifier))

      whenReady(future) { results =>
        results should have length (1)
        results.head.left.value shouldBe a[IndexNotFoundError]
      }
    }
  }
}
