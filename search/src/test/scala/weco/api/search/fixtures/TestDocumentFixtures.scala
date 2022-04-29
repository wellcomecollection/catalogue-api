package weco.api.search.fixtures

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Index
import io.circe.Json
import org.scalatest.Suite
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import weco.elasticsearch.test.fixtures.ElasticsearchFixtures
import weco.fixtures.LocalResources
import weco.json.JsonUtil._

import scala.util.{Failure, Success, Try}

trait TestDocumentFixtures extends ElasticsearchFixtures with LocalResources {
  this: Suite =>
  val visibleWorks = (0 to 4).map(i => s"works.visible.$i")
  val invisibleWorks = (0 to 2).map(i => s"works.invisible.$i")
  val redirectedWorks = (0 to 1).map(i => s"works.redirected.$i")
  val deletedWorks = (0 to 3).map(i => s"works.deleted.$i")

  val works = visibleWorks ++ invisibleWorks ++ redirectedWorks ++ deletedWorks

  val worksFormatBooks = (0 to 3).map(i => s"works.formats.$i.Books")
  val worksFormatJournals = (4 to 6).map(i => s"works.formats.$i.Journals")
  val worksFormatAudio = (7 to 8).map(i => s"works.formats.$i.Audio")
  val worksFormatPictures = (9 to 9).map(i => s"works.formats.$i.Pictures")

  val worksFormat = worksFormatBooks ++ worksFormatJournals ++ worksFormatAudio ++ worksFormatPictures

  val worksEverything = (0 to 2).map(i => s"work.visible.everything.$i")

  protected case class TestDocument(id: String, document: Json)

  def getTestDocuments(ids: Seq[String]): Seq[TestDocument] =
    ids.map { id =>
      val doc = Try { readResource(s"test_documents/$id.json") }
        .flatMap(jsonString => fromJson[TestDocument](jsonString))

      doc match {
        case Success(d) => d
        case Failure(err) =>
          throw new IllegalArgumentException(
            s"Unable to read fixture $id: $err"
          )
      }
    }

  def indexTestDocuments(
    index: Index,
    documentIds: String*
  ): Unit = {
    val documents = getTestDocuments(documentIds)

    val result = elasticClient.execute(
      bulk(
        documents.map { fixture =>
          indexInto(index.name)
            .id(fixture.id)
            .doc(fixture.document.noSpaces)
        }
      ).refreshImmediately
    )

    // With a large number of works this can take a long time
    // 30 seconds should be enough
    whenReady(result, Timeout(Span(30, Seconds))) { _ =>
      getSizeOf(index) shouldBe documents.size
    }
  }

  private def getSizeOf(index: Index): Long =
    elasticClient
      .execute { count(index.name) }
      .await
      .result
      .count
}
