package weco.api.snapshot_generator.storage

import org.apache.commons.io.FileUtils
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, TryValues}
import software.amazon.awssdk.services.s3.model.{
  HeadObjectRequest,
  NoSuchBucketException
}
import weco.fixtures.RandomGenerators
import weco.storage.fixtures.S3Fixtures
import weco.storage.store.s3.S3StreamStore
import weco.storage.streaming.StreamAssertions

class S3UploaderTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with TryValues
    with S3Fixtures
    with RandomGenerators
    with StreamAssertions {
  val s3StreamStore = new S3StreamStore()

  val uploader = new S3Uploader(partSize = (5 * FileUtils.ONE_MB).toInt)

  it("uploads a large file (>partSize)") {
    val bytes = randomBytes(length = (10 * FileUtils.ONE_MB).toInt + 5)

    withLocalS3Bucket { bucket =>
      val location = createS3ObjectLocationWith(bucket)

      val uploadResult =
        uploader.upload(location, bytes.toIterator).success.value

      uploadResult.bucket() shouldBe location.bucket
      uploadResult.key() shouldBe location.key

      val headRequest =
        HeadObjectRequest.builder()
          .bucket(location.bucket)
          .key(location.key)
          .build()

      s3Client.headObject(headRequest).eTag() shouldBe uploadResult.eTag()

      assertStreamEquals(
        s3StreamStore.get(location).right.value.identifiedT,
        bytes,
        expectedLength = bytes.length.toLong
      )
    }
  }

  it("fails if the bucket does not exist") {
    val bytes = randomBytes()

    val location = createS3ObjectLocation

    val uploadResult = uploader.upload(location, bytes.toIterator).failed.get

    uploadResult shouldBe a[NoSuchBucketException]
  }
}
