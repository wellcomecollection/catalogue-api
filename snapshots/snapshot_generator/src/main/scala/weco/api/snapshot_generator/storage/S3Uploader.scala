package weco.api.snapshot_generator.storage

import grizzled.slf4j.Logging
import org.apache.commons.io.FileUtils
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{
  CompleteMultipartUploadRequest,
  CompleteMultipartUploadResponse,
  CompletedMultipartUpload,
  CompletedPart,
  CreateMultipartUploadRequest,
  CreateMultipartUploadResponse,
  UploadPartRequest
}
import weco.storage.s3.S3ObjectLocation

import scala.collection.JavaConverters._
import scala.util.Try

class S3Uploader(partSize: Int = (5 * FileUtils.ONE_MB).toInt)(
  implicit s3Client: S3Client
) extends Logging {

  require(
    partSize >= 5 * FileUtils.ONE_MB,
    s"Parts must be at least 5 MB in size, got $partSize < ${5 * FileUtils.ONE_MB}; see https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html"
  )

  def upload(
    location: S3ObjectLocation,
    bytes: Iterator[Byte]
  ): Try[CompleteMultipartUploadResponse] = Try {

    val createRequest =
      CreateMultipartUploadRequest
        .builder()
        .bucket(location.bucket)
        .key(location.key)
        .build()

    val createResponse = s3Client.createMultipartUpload(createRequest)

    debug(
      s"Got CreateMultipartUploadResponse with upload ID ${createResponse.uploadId()}"
    )

    val completedParts = uploadParts(createResponse, location, bytes)

    val completedMultipartUpload =
      CompletedMultipartUpload
        .builder()
        .parts(completedParts.asJava)
        .build()

    val completeRequest =
      CompleteMultipartUploadRequest
        .builder()
        .bucket(location.bucket)
        .key(location.key)
        .uploadId(createResponse.uploadId())
        .multipartUpload(completedMultipartUpload)
        .build()

    s3Client.completeMultipartUpload(completeRequest)
  }

  private def uploadParts(
    createResponse: CreateMultipartUploadResponse,
    location: S3ObjectLocation,
    bytes: Iterator[Byte]
  ): List[CompletedPart] =
    bytes
      .grouped(partSize)
      .zipWithIndex
      .map {
        // Part numbers in S3 multi-part uploads are 1-indexed
        case (partBytes, index) => (partBytes, index + 1)
      }
      .map {
        case (partBytes, partNumber) =>
          val uploadPartRequest =
            UploadPartRequest
              .builder()
              .bucket(location.bucket)
              .key(location.key)
              .uploadId(createResponse.uploadId())
              .partNumber(partNumber)
              .build()

          val requestBody = RequestBody.fromBytes(partBytes.toArray)

          val uploadPartResponse =
            s3Client.uploadPart(uploadPartRequest, requestBody)

          CompletedPart
            .builder()
            .eTag(uploadPartResponse.eTag())
            .partNumber(partNumber)
            .build()
      }
      .toList
}
