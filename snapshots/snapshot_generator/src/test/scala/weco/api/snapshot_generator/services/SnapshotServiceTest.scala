package weco.api.snapshot_generator.services

import com.amazonaws.services.s3.model.AmazonS3Exception
import com.sksamuel.elastic4s.{ElasticClient, Index}
import com.sksamuel.elastic4s.http.JavaClientExceptionWrapper
import org.scalatest.TryValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.fixtures.TestDocumentFixtures
import weco.api.search.models.ApiVersions
import weco.api.snapshot_generator.fixtures.SnapshotServiceFixture
import weco.api.snapshot_generator.models.SnapshotJob
import weco.api.snapshot_generator.test.utils.S3GzipUtils
import weco.catalogue.display_model.work.DisplayWork
import weco.elasticsearch.ElasticClientBuilder
import weco.fixtures.TestWith
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.work.generators.WorkGenerators
import weco.storage.fixtures.S3Fixtures.Bucket
import weco.storage.s3.S3ObjectLocation
import weco.http.json.DisplayJsonUtil.toJson
import weco.json.utils.JsonAssertions

import java.time.Instant

class SnapshotServiceTest
    extends AnyFunSpec
    with Matchers
    with TryValues
    with S3GzipUtils
    with SnapshotServiceFixture
    with TestDocumentFixtures
    with JsonAssertions {

  import weco.catalogue.display_model.Implicits._

  def withFixtures[R](
    testWith: TestWith[(SnapshotService, Index, Bucket), R]
  ): R =
    withLocalWorksIndex { worksIndex =>
      withLocalS3Bucket { bucket =>
        withSnapshotService(worksIndex) { snapshotService =>
          testWith((snapshotService, worksIndex, bucket))
        }
      }
    }

  val expectedDisplayWorkClassName =
    "weco.catalogue.display_model.work.DisplayWork$"

  it("completes a snapshot generation") {
    withFixtures {
      case (snapshotService, worksIndex, bucket) =>
        indexTestDocuments(worksIndex, works: _*)

        val s3Location = S3ObjectLocation(bucket.name, key = "target.txt.gz")
        val snapshotJob = SnapshotJob(
          s3Location = s3Location,
          apiVersion = ApiVersions.v2,
          requestedAt = Instant.now()
        )

        val result = snapshotService.generateSnapshot(snapshotJob).success.value

        val (objectMetadata, contents) = getGzipObjectFromS3(s3Location)

        val s3Etag = objectMetadata.getETag
        val s3Size = objectMetadata.getContentLength

        val expectedJsonLines =
          readResource("expected-snapshot.txt").split("\n")
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
        result.snapshotResult.displayModel shouldBe expectedDisplayWorkClassName

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

  ignore(
    "completes a snapshot generation of an index with more than 10000 items"
  ) {
    // TODO: restore this test after we've refactored the snapshot generator
    // to use the "display" property on documents, so we can easily generate
    // 1000 examples to use here.
  }

  it("returns a failed future if the S3 upload fails") {
    withFixtures {
      case (snapshotService, worksIndex, _) =>
        indexTestDocuments(worksIndex, works: _*)

        val snapshotJob = SnapshotJob(
          s3Location = createS3ObjectLocation,
          apiVersion = ApiVersions.v2,
          requestedAt = Instant.now
        )

        val exc = snapshotService.generateSnapshot(snapshotJob).failed.get
        exc shouldBe a[AmazonS3Exception]
        exc.getMessage should startWith("The specified bucket does not exist")
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
        worksIndex = "wrong-index",
        elasticClient = brokenElasticClient
      ) { brokenSnapshotService =>
        val snapshotJob = SnapshotJob(
          s3Location = createS3ObjectLocationWith(bucket),
          apiVersion = ApiVersions.v2,
          requestedAt = Instant.now()
        )

        brokenSnapshotService
          .generateSnapshot(snapshotJob)
          .failed
          .get shouldBe a[JavaClientExceptionWrapper]
      }
    }
  }
}
