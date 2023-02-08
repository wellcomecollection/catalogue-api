package weco.api.snapshot_generator.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.http.JavaClientExceptionWrapper
import com.sksamuel.elastic4s.{ElasticClient, Index}
import io.circe.Json
import io.circe.generic.extras.JsonKey
import io.circe.syntax._
import org.scalatest.TryValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import weco.api.search.fixtures.TestDocumentFixtures
import weco.api.snapshot_generator.fixtures.SnapshotServiceFixture
import weco.api.snapshot_generator.test.utils.S3GzipUtils
import weco.elasticsearch.ElasticClientBuilder
import weco.fixtures.TestWith
import weco.json.JsonUtil._
import weco.json.utils.JsonAssertions
import weco.storage.fixtures.S3Fixtures.Bucket
import weco.storage.s3.S3ObjectLocation

class SnapshotServiceTest
    extends AnyFunSpec
    with Matchers
    with TryValues
    with S3GzipUtils
    with SnapshotServiceFixture
    with TestDocumentFixtures
    with JsonAssertions {

  def withFixtures[R](
    testWith: TestWith[(SnapshotService, Index, Bucket), R]
  ): R =
    withLocalWorksIndex { worksIndex =>
      withLocalS3Bucket { bucket =>
        withSnapshotService() { snapshotService =>
          testWith((snapshotService, worksIndex, bucket))
        }
      }
    }

  it("completes a snapshot generation") {
    withFixtures {
      case (snapshotService, worksIndex, bucket) =>
        indexTestDocuments(worksIndex, works: _*)

        val s3Location = S3ObjectLocation(bucket.name, key = "target.txt.gz")
        val snapshotJob = createSnapshotJob(
          s3Location,
          worksIndex,
          query = SnapshotServiceFixture.visibleTermQuery
        )

        val result = snapshotService.generateSnapshot(snapshotJob).success.value

        val (s3Size, s3Etag, contents) = getGzipObjectFromS3(s3Location)

        val expectedJsonLines =
          readResource("expected-snapshot-works.txt").split("\n")
        val actualLines = contents.split("\n")

        actualLines.zip(expectedJsonLines).foreach {
          case (actualLine, expectedLine) =>
            withClue(s"actualLine = <<$actualLine>>") {
              assertJsonStringsAreEqual(actualLine, expectedLine)
            }
        }

        result.snapshotJob shouldBe snapshotJob

        result.snapshotResult.indexName shouldBe worksIndex.name
        result.snapshotResult.documentCount shouldBe visibleWorks.length

        result.snapshotResult.startedAt shouldBe >=(
          result.snapshotJob.requestedAt
        )
        result.snapshotResult.finishedAt shouldBe >(
          result.snapshotResult.startedAt
        )

        result.snapshotResult.s3Etag shouldBe s3Etag
        result.snapshotResult.s3Size shouldBe s3Size
        result.snapshotResult.s3Location shouldBe s3Location
    }
  }

  it("completes a snapshot generation of an index with more than 10000 items") {
    withFixtures {
      case (snapshotService, worksIndex, bucket) =>
        case class HasDisplay(
          display: Json,
          @JsonKey("type") ontologyType: String
        )

        val documents = (1 to 10100)
          .map { i =>
            val id = i.toString
            val doc = HasDisplay(
              display = Json.fromString(s"document $i"),
              ontologyType = "Visible"
            )

            (id, doc)
          }

        elasticClient.execute(
          bulk(
            documents.map {
              case (id, doc) =>
                indexInto(worksIndex)
                  .id(id)
                  .doc(doc.asJson.noSpaces)
            }
          ).refreshImmediately
        )

        eventually {
          getSizeOf(worksIndex) shouldBe documents.length
        }

        val s3Location = S3ObjectLocation(bucket.name, key = "target.txt.gz")
        val snapshotJob = createSnapshotJob(
          s3Location,
          worksIndex,
          query = SnapshotServiceFixture.visibleTermQuery
        )

        val result = snapshotService.generateSnapshot(snapshotJob).success.value

        val (s3Size, s3Etag, contents) = getGzipObjectFromS3(s3Location)

        val expectedContents = documents
          .map { case (id, _) => s""""document $id"""" }
          .mkString("\n") + "\n"

        contents shouldBe expectedContents

        result.snapshotJob shouldBe snapshotJob

        result.snapshotResult.indexName shouldBe worksIndex.name
        result.snapshotResult.documentCount shouldBe documents.length

        result.snapshotResult.startedAt shouldBe >=(
          result.snapshotJob.requestedAt
        )
        result.snapshotResult.finishedAt shouldBe >=(
          result.snapshotResult.startedAt
        )

        result.snapshotResult.s3Etag shouldBe s3Etag
        result.snapshotResult.s3Size shouldBe s3Size
        result.snapshotResult.s3Location shouldBe s3Location
    }
  }

  it("returns a failed future if the S3 upload fails") {
    withFixtures {
      case (snapshotService, worksIndex, _) =>
        indexTestDocuments(worksIndex, works: _*)

        val snapshotJob = createSnapshotJob(
          createS3ObjectLocation,
          worksIndex,
          query = SnapshotServiceFixture.visibleTermQuery
        )

        val exc = snapshotService.generateSnapshot(snapshotJob).failed.get
        exc shouldBe a[NoSuchBucketException]
    }
  }

  it("returns a failed future if it fails reading from elasticsearch") {
    withLocalS3Bucket { bucket =>
      val brokenElasticClient: ElasticClient = ElasticClientBuilder.create(
        hostname = "localhost",
        port = 8888,
        protocol = "http",
        username = "elastic",
        password = "changeme"
      )

      withSnapshotService(
        elasticClient = brokenElasticClient
      ) { brokenSnapshotService =>
        val snapshotJob = createSnapshotJob(
          s3Location = createS3ObjectLocationWith(bucket),
          index = "not-an-index"
        )

        brokenSnapshotService
          .generateSnapshot(snapshotJob)
          .failed
          .get shouldBe a[JavaClientExceptionWrapper]
      }
    }
  }
}
