package weco.api.snapshot_generator.services

import com.amazonaws.services.s3.model.AmazonS3Exception
import com.sksamuel.elastic4s.{ElasticClient, Index}
import com.sksamuel.elastic4s.http.JavaClientExceptionWrapper
import org.scalatest.TryValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.snapshot_generator.fixtures.SnapshotServiceFixture
import weco.api.snapshot_generator.models.SnapshotJob
import weco.api.snapshot_generator.test.utils.S3GzipUtils
import weco.catalogue.display_model.models.DisplayWork
import weco.elasticsearch.ElasticClientBuilder
import weco.fixtures.TestWith
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.work.generators.WorkGenerators
import weco.storage.fixtures.S3Fixtures.Bucket
import weco.storage.s3.S3ObjectLocation
import weco.catalogue.display_model.models.{
  ApiVersions,
  DisplayWork,
  WorksIncludes
}
import weco.http.json.DisplayJsonUtil.toJson

import java.time.Instant

class SnapshotServiceTest
    extends AnyFunSpec
    with Matchers
    with TryValues
    with S3GzipUtils
    with SnapshotServiceFixture
    with WorkGenerators {

  import weco.catalogue.display_model.models.Implicits._

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
    "weco.catalogue.display_model.models.DisplayWork$"

  it("completes a snapshot generation") {
    withFixtures {
      case (snapshotService, worksIndex, bucket) =>
        val visibleWorks = indexedWorks(count = 3)
        val notVisibleWorks = indexedWorks(count = 2).map {
          _.invisible()
        }

        val works = visibleWorks ++ notVisibleWorks
        insertIntoElasticsearch(worksIndex, works: _*)

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

        val expectedContents = visibleWorks
          .map {
            DisplayWork(_, includes = WorksIncludes.all)
          }
          .map(toJson[DisplayWork])
          .mkString("\n") + "\n"

        contents shouldBe expectedContents

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

  it("completes a snapshot generation of an index with more than 10000 items") {
    withFixtures {
      case (snapshotService, worksIndex, bucket) =>
        val works = indexedWorks(count = 11000)
          .map {
            _.title(randomAlphanumeric(length = 1500))
          }

        insertIntoElasticsearch(worksIndex, works: _*)

        val s3Location = S3ObjectLocation(bucket.name, key = "target.txt.gz")
        val snapshotJob = SnapshotJob(
          s3Location = s3Location,
          apiVersion = ApiVersions.v2,
          requestedAt = Instant.now()
        )

        val result = snapshotService.generateSnapshot(snapshotJob).success.value

        val (objectMetadata, contents) = getGzipObjectFromS3(s3Location)
        import weco.catalogue.display_model.models.Implicits._

        val s3Etag = objectMetadata.getETag
        val s3Size = objectMetadata.getContentLength

        val expectedContents = works
          .map {
            DisplayWork(_, includes = WorksIncludes.all)
          }
          .map(toJson[DisplayWork])
          .mkString("\n") + "\n"

        contents shouldBe expectedContents

        result.snapshotJob shouldBe snapshotJob

        result.snapshotResult.indexName shouldBe worksIndex.name
        result.snapshotResult.documentCount shouldBe works.length
        result.snapshotResult.displayModel shouldBe expectedDisplayWorkClassName

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
        val works = indexedWorks(count = 3)

        insertIntoElasticsearch(worksIndex, works: _*)

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
