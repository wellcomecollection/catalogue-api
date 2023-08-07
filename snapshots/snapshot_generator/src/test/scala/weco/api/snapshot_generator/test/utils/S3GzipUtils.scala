package weco.api.snapshot_generator.test.utils

import java.io.File
import software.amazon.awssdk.services.s3.model.{
  GetObjectRequest,
  HeadObjectRequest
}
import weco.storage.fixtures.S3Fixtures
import weco.storage.providers.s3.S3ObjectLocation

trait S3GzipUtils extends GzipUtils with S3Fixtures {
  def getGzipObjectFromS3(
    location: S3ObjectLocation
  ): (Long, String, String) = {
    val downloadFile =
      File.createTempFile("snapshotServiceTest", ".txt.gz")

    // The GetObject call will fail if there's already a file at the path, so
    // delete it and let S3 write it.
    downloadFile.delete()

    val getRequest =
      GetObjectRequest.builder()
        .bucket(location.bucket)
        .key(location.key)
        .build()

    s3Client.getObject(getRequest, downloadFile.toPath)

    val headRequest =
      HeadObjectRequest.builder()
        .bucket(location.bucket)
        .key(location.key)
        .build()

    val headResponse = s3Client.headObject(headRequest)

    (headResponse.contentLength(), headResponse.eTag(), readGzipFile(downloadFile.getPath))
  }
}
