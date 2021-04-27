package uk.ac.wellcome.platform.snapshot_generator.akkastreams.sink

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import org.apache.commons.io.FileUtils
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.fixtures.RandomGenerators
import uk.ac.wellcome.platform.snapshot_generator.fixtures.AkkaS3
import uk.ac.wellcome.storage.store.s3.S3StreamStore
import uk.ac.wellcome.storage.streaming.StreamAssertions

import java.io.ByteArrayInputStream

class S3SinkTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with Akka
    with AkkaS3
    with RandomGenerators
    with ScalaFutures
    with IntegrationPatience
    with StreamAssertions {
  val s3StreamStore = new S3StreamStore()

  it("uploads a large file (>20MB)") {
    val bytes = randomBytes(length = (20 * FileUtils.ONE_MB).toInt)
    val inputStream = new ByteArrayInputStream(bytes)

    val source: Source[ByteString, Any] =
      StreamConverters.fromInputStream(() => inputStream)

    withActorSystem { implicit actorSystem =>
      withLocalS3Bucket { bucket =>
        val location = createS3ObjectLocationWith(bucket)

        withS3AkkaSettings(endpoint = localS3EndpointUrl) { settings =>
          val future = source.runWith(S3Sink(settings)(location))

          whenReady(future) { uploadResult =>
            uploadResult.bucket shouldBe location.bucket
            uploadResult.key shouldBe location.key

            s3Client
              .getObjectMetadata(location.bucket, location.key)
              .getETag shouldBe uploadResult.etag

            assertStreamEquals(
              s3StreamStore.get(location).right.value.identifiedT,
              bytes,
              expectedLength = bytes.length.toLong
            )
          }
        }
      }
    }
  }

  it("fails if the bucket does not exist") {
    val bytes = randomBytes(length = (20 * FileUtils.ONE_MB).toInt)
    val inputStream = new ByteArrayInputStream(bytes)

    val source: Source[ByteString, Any] =
      StreamConverters.fromInputStream(() => inputStream)

    withActorSystem { implicit actorSystem =>
      val location = createS3ObjectLocation

      withS3AkkaSettings(endpoint = localS3EndpointUrl) { settings =>
        val future = source.runWith(S3Sink(settings)(location))

        whenReady(future.failed) { err =>
          err.getMessage shouldBe "The specified bucket does not exist."
        }
      }
    }
  }
}
