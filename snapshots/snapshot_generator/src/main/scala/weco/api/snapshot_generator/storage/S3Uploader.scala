package weco.api.snapshot_generator.storage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{
  CompleteMultipartUploadRequest,
  CompleteMultipartUploadResult,
  InitiateMultipartUploadRequest,
  InitiateMultipartUploadResult,
  UploadPartRequest,
  UploadPartResult
}
import grizzled.slf4j.Logging
import org.apache.commons.io.FileUtils
import weco.storage.s3.S3ObjectLocation

import java.io.ByteArrayInputStream
import scala.collection.JavaConverters._
import scala.util.Try

class S3Uploader(partSize: Int = (5 * FileUtils.ONE_MB).toInt)(
  implicit s3Client: AmazonS3
) extends Logging {

  require(
    partSize >= 5 * FileUtils.ONE_MB,
    s"Parts must be at least 5 MB in size, got $partSize < ${5 * FileUtils.ONE_MB}; see https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html"
  )

  def upload(
    location: S3ObjectLocation,
    bytes: Iterator[Byte]
  ): Try[CompleteMultipartUploadResult] = Try {
    val initResponse = s3Client.initiateMultipartUpload(
      new InitiateMultipartUploadRequest(location.bucket, location.key)
    )

    debug(
      s"Got init response for MultiPart Upload: upload ID ${initResponse.getUploadId}"
    )

    val partUploadResults = uploadParts(initResponse, location, bytes)

    s3Client.completeMultipartUpload(
      new CompleteMultipartUploadRequest()
        .withBucketName(location.bucket)
        .withKey(location.key)
        .withUploadId(initResponse.getUploadId)
        .withPartETags(partUploadResults.asJava)
    )
  }

  private def uploadParts(
    initResponse: InitiateMultipartUploadResult,
    location: S3ObjectLocation,
    bytes: Iterator[Byte]
  ): List[UploadPartResult] =
    bytes
      .grouped(partSize)
      .zipWithIndex
      .map {
        // Part numbers in S3 multi-part uploads are 1-indexed
        case (partBytes, index) => (partBytes, index + 1)
      }
      .map {
        case (partBytes, partNumber) =>
          s3Client.uploadPart(
            new UploadPartRequest()
              .withBucketName(location.bucket)
              .withKey(location.key)
              .withUploadId(initResponse.getUploadId)
              .withPartNumber(partNumber)
              .withInputStream(new ByteArrayInputStream(partBytes.toArray))
              .withPartSize(partBytes.size)
          )
      }
      .toList
}
