package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticError
import com.sksamuel.elastic4s.circe._
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.elasticsearch.NoStrictMapping
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.json.JsonUtil._
import weco.catalogue.internal_model.generators.IdentifiersGenerators
import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.concurrent.ExecutionContext.Implicits.global

class ElasticLookupTest
  extends AnyFunSpec
    with Matchers
    with EitherValues
    with ElasticsearchFixtures
    with IdentifiersGenerators {

  case class Shape(id: CanonicalId, color: String, sides: Int)

  val elasticLookup = new ElasticLookup[Shape]()

  it("looks up an object by ID") {
    val redSquare = Shape(id = createCanonicalId, color = "red", sides = 4)

    withLocalElasticsearchIndex(NoStrictMapping) { index =>
      elasticClient.execute(
        indexInto(index).id(redSquare.id.toString).doc(redSquare)
      ).await

      val future = elasticLookup.lookupById(redSquare.id)(index)

      whenReady(future) {
        _ shouldBe Right(Some(redSquare))
      }
    }
  }

  it("returns None if the ID doesn't exist") {
    withLocalElasticsearchIndex(NoStrictMapping) { index =>
      val future = elasticLookup.lookupById(createCanonicalId)(index)

      whenReady(future) {
        _ shouldBe Right(None)
      }
    }
  }

  it("returns a failed future if it can't deserialise the object") {
    case class BadShape(id: CanonicalId, color: String, sides: String)

    val blueTriangle = BadShape(id = createCanonicalId, color = "blue", sides = "three")

    withLocalElasticsearchIndex(NoStrictMapping) { index =>
      elasticClient.execute(
        indexInto(index).id(blueTriangle.id.toString).doc(blueTriangle)
      ).await

      val future = elasticLookup.lookupById(blueTriangle.id)(index)

      whenReady(future.failed) { err =>
        err shouldBe a[RuntimeException]
        err.getMessage should startWith("Unable to parse JSON")
      }
    }
  }

  it("returns a Left[ElasticError] if it Elasticsearch returns an error") {
    val future = elasticLookup.lookupById(createCanonicalId)(createIndex)

    whenReady(future) {
      _.left.value shouldBe a[ElasticError]
    }
  }
}
