package weco.api.snapshot_generator.iterators

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import io.circe.Json
import io.circe.generic.extras.JsonKey
import io.circe.syntax._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.fixtures.IndexFixtures
import weco.api.snapshot_generator.fixtures.SnapshotServiceFixture
import weco.json.JsonUtil._

class ElasticsearchIteratorTest
    extends AnyFunSpec
    with Matchers
    with IndexFixtures {

  implicit val client: ElasticClient = elasticClient
  val iterator = new ElasticsearchIterator()

  case class HasDisplay(display: Json, @JsonKey("type") ontologyType: String)

  it("returns all the visible documents in the index") {
    withLocalWorksIndex { index =>
      val visibleDocuments = (1 to 10).map { i =>
        HasDisplay(
          display = Json.fromString(s"document $i"),
          ontologyType = "Visible"
        )
      }

      val invisibleDocuments = (11 to 15).map { i =>
        HasDisplay(
          display = Json.fromString(s"document $i"),
          ontologyType = "Invisible"
        )
      }

      val documents = visibleDocuments ++ invisibleDocuments

      elasticClient.execute(
        bulk(
          documents.map { doc =>
            indexInto(index).doc(doc.asJson.noSpaces)
          }
        ).refreshImmediately
      )

      eventually {
        getSizeOf(index) shouldBe documents.length
      }

      iterator
        .scroll(
          index = index,
          bulkSize = 1000,
          query = Some("""{ "term": { "type": "Visible" } }""")
        )
        .toList should contain theSameElementsAs (1 to 10)
        .map(i => s""""document $i"""")
    }
  }

  it("fetches more works than the bulk size") {
    withLocalWorksIndex { index =>
      val documents = (1 to 10).map { i =>
        HasDisplay(
          display = Json.fromString(s"document $i"),
          ontologyType = "Visible"
        )
      }

      elasticClient.execute(
        bulk(
          documents.map { doc =>
            indexInto(index).doc(doc.asJson.noSpaces)
          }
        ).refreshImmediately
      )

      eventually {
        getSizeOf(index) shouldBe documents.length
      }

      iterator
        .scroll(
          index = index,
          bulkSize = 1000,
          query = SnapshotServiceFixture.visibleTermQuery
        )
        .toList should contain theSameElementsAs (1 to 10)
        .map(i => s""""document $i"""")
    }
  }
}
