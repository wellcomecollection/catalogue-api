package weco.api.stacks.services

import com.sksamuel.elastic4s.ElasticError
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.index.IndexFixtures
import uk.ac.wellcome.models.work.generators.{ItemsGenerators, WorkGenerators}
import weco.api.search.elasticsearch.ElasticsearchService

import scala.concurrent.ExecutionContext.Implicits.global

class ItemLookupTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with IndexFixtures
    with ItemsGenerators
    with WorkGenerators {
  val lookup = new ItemLookup(
    elasticsearchService = new ElasticsearchService(elasticClient)
  )

  describe("byCanonicalId") {
    it("finds a work with the same item ID") {
      val item1 = createIdentifiedItem
      val item2 = createIdentifiedItem
      val item3 = createIdentifiedItem

      val workA = indexedWork().items(List(item1, item2))
      val workB = indexedWork().items(List(item2, item3))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, workA, workB)

        val future1 = lookup.byCanonicalId(item1.id.canonicalId)(index)

        whenReady(future1) {
          _ shouldBe Right(Some(item1))
        }

        val future2 = lookup.byCanonicalId(item2.id.canonicalId)(index)

        whenReady(future2) {
          _ shouldBe Right(Some(item2))
        }

        val future3 = lookup.byCanonicalId(item3.id.canonicalId)(index)

        whenReady(future3) {
          _ shouldBe Right(Some(item3))
        }
      }
    }

    it("returns None if there is no such work") {
      withLocalWorksIndex { index =>
        val future = lookup.byCanonicalId(createCanonicalId)(index)

        whenReady(future) {
          _ shouldBe Right(None)
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

        val future1 = lookup.byCanonicalId(item.id.canonicalId)(index)

        whenReady(future1) {
          _ shouldBe Right(None)
        }

        // Then we index both works and run the same query, so we know the
        // invisibility of the first work was the reason it was hidden.
        insertIntoElasticsearch(index, workInvisible, workVisible)

        val future2 = lookup.byCanonicalId(item.id.canonicalId)(index)

        whenReady(future2) {
          _ shouldBe Right(Some(item))
        }
      }
    }

    it("returns Left[Error] if Elasticsearch has an error") {
      val future = lookup.byCanonicalId(createCanonicalId)(createIndex)

      whenReady(future) {
        _.left.value shouldBe a[ElasticError]
      }
    }
  }

  describe("bySourceIdentifier") {
    it("finds a work with the same item ID") {
      val item1 = createIdentifiedItem
      val item2 = createIdentifiedItem
      val item3 = createIdentifiedItem

      val workA = indexedWork().items(List(item1, item2))
      val workB = indexedWork().items(List(item2, item3))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, workA, workB)

        val future1 =
          lookup.bySourceIdentifier(item1.id.sourceIdentifier)(index)

        whenReady(future1) {
          _ shouldBe Right(Some(item1))
        }

        val future2 =
          lookup.bySourceIdentifier(item2.id.sourceIdentifier)(index)

        whenReady(future2) {
          _ shouldBe Right(Some(item2))
        }

        val future3 =
          lookup.bySourceIdentifier(item3.id.sourceIdentifier)(index)

        whenReady(future3) {
          _ shouldBe Right(Some(item3))
        }
      }
    }

    it("finds a work with the same item ID in the otherIdentifiers field") {
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

        val future1 =
          lookup.bySourceIdentifier(item1.id.otherIdentifiers.head)(index)

        whenReady(future1) {
          _ shouldBe Right(Some(item1))
        }

        val future2 =
          lookup.bySourceIdentifier(item2.id.otherIdentifiers.head)(index)

        whenReady(future2) {
          _ shouldBe Right(Some(item2))
        }

        val future3 =
          lookup.bySourceIdentifier(item3.id.otherIdentifiers.head)(index)

        whenReady(future3) {
          _ shouldBe Right(Some(item3))
        }
      }
    }

    it("returns None if there is no such work") {
      withLocalWorksIndex { index =>
        val future = lookup.bySourceIdentifier(createSourceIdentifier)(index)

        whenReady(future) {
          _ shouldBe Right(None)
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

        val future1 = lookup.bySourceIdentifier(item.id.sourceIdentifier)(index)

        whenReady(future1) {
          _ shouldBe Right(None)
        }

        // Then we index both works and run the same query, so we know the
        // invisibility of the first work was the reason it was hidden.
        insertIntoElasticsearch(index, workInvisible, workVisible)

        val future2 = lookup.bySourceIdentifier(item.id.sourceIdentifier)(index)

        whenReady(future2) {
          _ shouldBe Right(Some(item))
        }
      }
    }

    it("returns Left[Error] if Elasticsearch has an error") {
      val future =
        lookup.bySourceIdentifier(createSourceIdentifier)(createIndex)

      whenReady(future) {
        _.left.value shouldBe a[ElasticError]
      }
    }
  }
}
