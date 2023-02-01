package weco.api.snapshot_generator

import com.sksamuel.elastic4s.Index
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.akka.fixtures.Akka
import weco.api.search.fixtures.TestDocumentFixtures
import weco.api.snapshot_generator.fixtures.{SnapshotServiceFixture, WorkerServiceFixture}
import weco.api.snapshot_generator.models.{CompletedSnapshotJob, SnapshotJob}
import weco.api.snapshot_generator.test.utils.S3GzipUtils
import weco.fixtures.TestWith
import weco.json.JsonUtil._
import weco.json.utils.JsonAssertions
import weco.messaging.fixtures.SQS.Queue
import weco.messaging.memory.MemoryMessageSender
import weco.storage.fixtures.S3Fixtures.Bucket
import weco.storage.s3.S3ObjectLocation

import java.time.Instant

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

  it("completes a snapshot generation") {
    withFixtures {
      case (queue, messageSender, worksIndex, bucket) =>
        indexTestDocuments(worksIndex, works: _*)

        val s3Location = S3ObjectLocation(bucket.name, key = "target.tar.gz")

        val snapshotJob = SnapshotJob(
          s3Location = s3Location,
          requestedAt = Instant.now(),
          index = worksIndex,
          bulkSize = 1000,
          query = SnapshotServiceFixture.visibleTermQuery
        )

        sendNotificationToSQS(queue = queue, message = snapshotJob)

        eventually {
          val (s3Size, s3Etag, contents) = getGzipObjectFromS3(s3Location)

          val actualJsonLines = contents.split("\n").toList

          val expectedJsonLines =
            readResource("expected-snapshot.txt").split("\n")

          actualJsonLines.zip(expectedJsonLines).foreach {
            case (actualLine, expectedLine) =>
              withClue(s"actualLine = <<$actualLine>>") {
                assertJsonStringsAreEqual(actualLine, expectedLine)
              }
          }

          val result = messageSender.getMessages[CompletedSnapshotJob].head

          result.snapshotJob shouldBe snapshotJob

          result.snapshotResult.indexName shouldBe worksIndex.name
          result.snapshotResult.documentCount shouldBe visibleWorks.length

          result.snapshotResult.startedAt shouldBe >(
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
  }

  def withFixtures[R](
    testWith: TestWith[(Queue, MemoryMessageSender, Index, Bucket), R]
  ): R =
    withActorSystem { implicit actorSystem =>
      withLocalSqsQueue() { queue =>
        val messageSender = new MemoryMessageSender()

        withLocalWorksIndex { worksIndex =>
          withLocalS3Bucket { bucket =>
            withWorkerService(queue, messageSender) { _ =>
              testWith((queue, messageSender, worksIndex, bucket))
            }
          }
        }
      }
    }
}
