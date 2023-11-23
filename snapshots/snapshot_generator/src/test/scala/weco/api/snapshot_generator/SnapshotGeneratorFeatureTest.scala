package weco.api.snapshot_generator

import com.sksamuel.elastic4s.Index
import org.scalatest.Assertion
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.akka.fixtures.Akka
import weco.api.search.fixtures.TestDocumentFixtures
import weco.api.snapshot_generator.fixtures.{
  SnapshotServiceFixture,
  WorkerServiceFixture
}
import weco.api.snapshot_generator.models.{CompletedSnapshotJob, SnapshotJob}
import weco.api.snapshot_generator.test.utils.S3GzipUtils
import weco.fixtures.TestWith
import weco.json.JsonUtil._
import weco.json.utils.JsonAssertions
import weco.messaging.fixtures.SQS.Queue
import weco.messaging.memory.MemoryMessageSender
import weco.storage.fixtures.S3Fixtures.Bucket
import weco.storage.providers.s3.S3ObjectLocation

class SnapshotGeneratorFeatureTest
    extends AnyFunSpec
    with Eventually
    with Matchers
    with Akka
    with S3GzipUtils
    with JsonAssertions
    with IntegrationPatience
    with WorkerServiceFixture
    with TestDocumentFixtures {

  it("completes a snapshot generation for works and images") {
    withFixtures {
      case (queue, messageSender, worksIndex, imagesIndex, bucket) =>
        val images = (1 to 5).map(i => s"images.similar-features.$i")
        indexTestDocuments(imagesIndex, images: _*)
        indexTestDocuments(worksIndex, works: _*)

        val worksS3Location =
          S3ObjectLocation(bucket.name, key = "works.tar.gz")
        val imagesS3Location =
          S3ObjectLocation(bucket.name, key = "images.tar.gz")

        val worksSnapshotJob = createSnapshotJob(
          worksS3Location,
          worksIndex,
          query = SnapshotServiceFixture.visibleTermQuery
        )
        val imagesSnapshotJob = createSnapshotJob(
          imagesS3Location,
          imagesIndex,
          query = None
        )

        sendNotificationToSQS(queue = queue, message = worksSnapshotJob)
        sendNotificationToSQS(queue = queue, message = imagesSnapshotJob)

        eventually {
          def checkSnapshotResult(
            resourceName: String,
            index: Index,
            nDocuments: Int,
            s3ObjectLocation: S3ObjectLocation,
            initialJob: SnapshotJob,
            result: CompletedSnapshotJob
          ): Assertion = {
            val (s3Size, s3Etag, contents) =
              getGzipObjectFromS3(s3ObjectLocation)
            val actualJsonLines = contents.split("\n").toList

            val expectedJsonLines =
              readResource(resourceName).split("\n")

            actualJsonLines.length shouldBe expectedJsonLines.length

            actualJsonLines.zip(expectedJsonLines).foreach {
              case (actualLine, expectedLine) =>
                withClue(s"actualLine = <<$actualLine>>") {
                  assertJsonStringsAreEqual(actualLine, expectedLine)
                }
            }

            result.snapshotJob shouldBe initialJob

            result.snapshotResult.indexName shouldBe index.name
            result.snapshotResult.documentCount shouldBe nDocuments

            result.snapshotResult.startedAt shouldBe >(
              result.snapshotJob.requestedAt
            )
            result.snapshotResult.finishedAt shouldBe >(
              result.snapshotResult.startedAt
            )

            result.snapshotResult.s3Etag shouldBe s3Etag
            result.snapshotResult.s3Size shouldBe s3Size
            result.snapshotResult.s3Location shouldBe s3ObjectLocation
          }

          val results = messageSender.getMessages[CompletedSnapshotJob]
          val completedImagesSnapshotJob =
            results.find(_.snapshotJob.index == imagesIndex).get
          val completedWorksSnapshotJob =
            results.find(_.snapshotJob.index == worksIndex).get

          checkSnapshotResult(
            resourceName = "expected-snapshot-works.txt",
            index = worksIndex,
            nDocuments = visibleWorks.length,
            s3ObjectLocation = worksS3Location,
            initialJob = worksSnapshotJob,
            result = completedWorksSnapshotJob
          )
          checkSnapshotResult(
            resourceName = "expected-snapshot-images.txt",
            index = imagesIndex,
            nDocuments = images.length,
            s3ObjectLocation = imagesS3Location,
            initialJob = imagesSnapshotJob,
            result = completedImagesSnapshotJob
          )
        }
    }
  }

  def withFixtures[R](
    testWith: TestWith[(Queue, MemoryMessageSender, Index, Index, Bucket), R]
  ): R =
    withActorSystem { implicit actorSystem =>
      withLocalSqsQueue() { queue =>
        val messageSender = new MemoryMessageSender()

        withLocalWorksIndex { worksIndex =>
          withLocalImagesIndex { imagesIndex =>
            withLocalS3Bucket { bucket =>
              withWorkerService(queue, messageSender) { _ =>
                testWith(
                  (queue, messageSender, worksIndex, imagesIndex, bucket)
                )
              }
            }
          }
        }
      }
    }
}
