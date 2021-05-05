package weco.api.stacks.services

import com.sksamuel.elastic4s.ElasticError
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.index.IndexFixtures
import uk.ac.wellcome.models.work.generators.WorkGenerators
import weco.api.search.elasticsearch.ElasticsearchService

import scala.concurrent.ExecutionContext.Implicits.global

class WorkLookupTest extends AnyFunSpec with Matchers with EitherValues with IndexFixtures with WorkGenerators {
  val lookup = new WorkLookup(
    elasticsearchService = new ElasticsearchService(elasticClient)
  )

  it("returns a work with matching ID") {
    withLocalWorksIndex { index =>
      val work = indexedWork()

      insertIntoElasticsearch(index, work)

      val future = lookup.byId(work.state.canonicalId)(index)

      whenReady(future) {
        _ shouldBe Right(Some(work))
      }
    }
  }

  it("returns None if there is no such work") {
    withLocalWorksIndex { index =>
      val future = lookup.byId(createCanonicalId)(index)

      whenReady(future) {
        _ shouldBe Right(None)
      }
    }
  }

  it("returns Left[ElasticError] if Elasticsearch has an error") {
    val future = lookup.byId(createCanonicalId)(createIndex)

    whenReady(future) {
      _.left.value shouldBe a[ElasticError]
    }
  }
}
