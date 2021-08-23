package weco.api.items.services

import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.elasticsearch.{DocumentNotFoundError, IndexNotFoundError}
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.index.IndexFixtures
import weco.catalogue.internal_model.work.generators.WorkGenerators

import scala.concurrent.ExecutionContext.Implicits.global

class WorkLookupTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with IndexFixtures
    with WorkGenerators {
  val lookup: WorkLookup = WorkLookup(elasticClient)

  it("returns a work with matching ID") {
    withLocalWorksIndex { index =>
      val work = indexedWork()

      insertIntoElasticsearch(index, work)

      val future = lookup.byCanonicalId(work.state.canonicalId)(index)

      whenReady(future) {
        _ shouldBe Right(work)
      }
    }
  }

  it("returns None if there is no such work") {
    withLocalWorksIndex { index =>
      val id = createCanonicalId
      val future = lookup.byCanonicalId(id)(index)

      whenReady(future) {
        _ shouldBe Left(DocumentNotFoundError(id))
      }
    }
  }

  it("returns Left[ElasticError] if Elasticsearch has an error") {
    val index = createIndex
    val future = lookup.byCanonicalId(createCanonicalId)(index)

    whenReady(future) { err =>
      err.left.value shouldBe a[IndexNotFoundError]
      err.left.value.asInstanceOf[IndexNotFoundError].index shouldBe index.name
    }
  }
}