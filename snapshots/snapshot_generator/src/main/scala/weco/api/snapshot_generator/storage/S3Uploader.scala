package weco.api.snapshot_generator.storage

import org.apache.commons.io.FileUtils
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{
  CompleteMultipartUploadResponse,
  CompletedPart,
  UploadPartRequest
}
import weco.storage.providers.s3.S3ObjectLocation
import weco.storage.store.s3.S3MultipartUploader

import scala.util.Try

class S3Uploader(val partSize: Int = (5 * FileUtils.ONE_MB).toInt)(
  implicit val s3Client: S3Client
) extends S3MultipartUploader {

  def upload(location: S3ObjectLocation,
             bytes: Iterator[Byte]): Try[CompleteMultipartUploadResponse] =
    for {
      uploadId <- createMultipartUpload(location)
      completedParts <- uploadParts(location, uploadId, bytes)
      response <- completeMultipartUpload(location, uploadId, completedParts)
    } yield response

  private def uploadParts(
    location: S3ObjectLocation,
    uploadId: String,
    bytes: Iterator[Byte]
  ): Try[List[CompletedPart]] = Try {
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
              .uploadId(uploadId)
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
}
