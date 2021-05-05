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

class WorksLookupTest extends AnyFunSpec with Matchers with EitherValues with IndexFixtures with ItemsGenerators with WorkGenerators {
  val lookup = new WorksLookup(
    elasticsearchService = new ElasticsearchService(elasticClient)
  )

  describe("byWorkId") {
    it("returns a work with matching ID") {
      withLocalWorksIndex { index =>
        val work = indexedWork()

        insertIntoElasticsearch(index, work)

        val future = lookup.byWorkId(work.state.canonicalId)(index)

        whenReady(future) {
          _ shouldBe Right(Some(work))
        }
      }
    }

    it("returns None if there is no such work") {
      withLocalWorksIndex { index =>
        val future = lookup.byWorkId(createCanonicalId)(index)

        whenReady(future) {
          _ shouldBe Right(None)
        }
      }
    }

    it("returns Left[ElasticError] if Elasticsearch has an error") {
      val future = lookup.byWorkId(createCanonicalId)(createIndex)

      whenReady(future) {
        _.left.value shouldBe a[ElasticError]
      }
    }
  }

  describe("byItemId") {
    it("finds a work with the same item ID") {
      val item1 = createIdentifiedItem
      val item2 = createIdentifiedItem
      val item3 = createIdentifiedItem

      val workA = indexedWork().items(List(item1, item2))
      val workB = indexedWork().items(List(item2, item3))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, workA, workB)

        val future1 = lookup.byItemId(item1.id.canonicalId)(index)

        whenReady(future1) {
          _ shouldBe Right(Some(workA))
        }

        val future2 = lookup.byItemId(item2.id.canonicalId)(index)

        whenReady(future2) { result =>
          val expectedResults = List(Right(Some(workA)), Right(Some(workB)))
          expectedResults should contain(result)
        }

        val future3 = lookup.byItemId(item3.id.canonicalId)(index)

        whenReady(future3) {
          _ shouldBe Right(Some(workB))
        }
      }
    }

    it("returns None if there is no such work") {
      withLocalWorksIndex { index =>
        val future = lookup.byItemId(createCanonicalId)(index)

        whenReady(future) {
          _ shouldBe Right(None)
        }
      }
    }

    it("returns None if there is no visible work") {
      val item = createIdentifiedItem

      val workInvisible = indexedWork().items(List(item)).invisible()
      val workVisible = indexedWork().items(List(item))

      withLocalWorksIndex { index =>
        // First we index just the invisible work, and check we don't
        // get any results.
        insertIntoElasticsearch(index, workInvisible)

        val future1 = lookup.byItemId(item.id.canonicalId)(index)

        whenReady(future1) {
          _ shouldBe Right(None)
        }

        // Then we index both works and run the same query, so we know the
        // invisibility of the first work was the reason it was hidden.
        insertIntoElasticsearch(index, workInvisible, workVisible)

        val future2 = lookup.byItemId(item.id.canonicalId)(index)

        whenReady(future2) {
          _ shouldBe Right(Some(workVisible))
        }
      }
    }

    it("returns Left[Error] if Elasticsearch has an error") {
      val future = lookup.byItemId(createCanonicalId)(createIndex)

      whenReady(future) {
        _.left.value shouldBe a[ElasticError]
      }
    }
  }
}
