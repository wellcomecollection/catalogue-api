package weco.api.requests.services

import com.sksamuel.elastic4s.Index
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.elasticsearch.{DocumentNotFoundError, IndexNotFoundError}
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.identifiers.IdentifierType.{MiroImageNumber, SierraSystemNumber}
import weco.catalogue.internal_model.index.IndexFixtures
import weco.catalogue.internal_model.work.Item
import weco.catalogue.internal_model.work.generators.{ItemsGenerators, WorkGenerators}

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

      val workA = indexedWork().items(List(item1, item2))
      val workB = indexedWork().items(List(item2, item3))

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
    it("finds items with the same item IDs") {
      val item1 = createIdentifiedItem
      val item2 = createIdentifiedItem
      val item3 = createIdentifiedItem

      val workA = indexedWork().items(List(item1, item2))
      val workB = indexedWork().items(List(item2, item3))

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
          _ shouldBe List(Right(item1), Right(item2))
        }
      }
    }

    it("returns not found errors where ID matches cannot be found") {
      val item1 = createIdentifiedItem
      val item2 = createIdentifiedItem
      val item3 = createIdentifiedItem
      val item4 = createIdentifiedItem

      val workA = indexedWork().items(List(item1, item2))
      val workB = indexedWork().items(List(item2, item3))

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
            Right(item1),
            Left(DocumentNotFoundError(item4.id.sourceIdentifier)),
            Right(item3)
          )
        }
      }
    }

    it("matches on all parts of the item ID") {
      val sourceIdentifier1 = createSourceIdentifierWith(
        identifierType = SierraSystemNumber,
        ontologyType = "Item"
      )
      val sourceIdentifier2 = sourceIdentifier1.copy(
        ontologyType = "Work"
      )
      val sourceIdentifier3 = sourceIdentifier1.copy(
        identifierType = MiroImageNumber
      )

      val item1 = createIdentifiedItemWith(sourceIdentifier = sourceIdentifier1)
      val item2 = createIdentifiedItemWith(sourceIdentifier = sourceIdentifier2)
      val item3 = createIdentifiedItemWith(sourceIdentifier = sourceIdentifier3)

      val workA = indexedWork().items(List(item1, item2))
      val workB = indexedWork().items(List(item2, item3))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, workA, workB)

        val lookup = createLookup(index)

        Seq(item1, item2, item3).foreach { item =>
          val future = lookup.bySourceIdentifier(Seq(item.id.sourceIdentifier))

          whenReady(future) {
            _ shouldBe Seq(Right(item))
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

      val workA = indexedWork().items(List(item1, item2))
      val workB = indexedWork().items(List(item2, item3))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, workA, workB)

        val lookup = createLookup(index)

        List(item1, item2, item3).foreach { item =>
          whenReady(lookup.bySourceIdentifier(Seq(item.id.sourceIdentifier))) {
            _ shouldBe Seq(Right(item))
          }

          whenReady(
            lookup.bySourceIdentifier(Seq(item.id.otherIdentifiers.head))
          ) {
            _ shouldBe Seq(
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
          _ shouldBe Seq(Right(item))
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
