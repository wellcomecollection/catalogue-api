package weco.elasticsearch

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.requests.searches.{SearchIterator, SearchRequest}
import grizzled.slf4j.Logging
import io.circe.Decoder

import java.text.NumberFormat
import scala.concurrent.duration._

/** @param bulkSize
  *   How many documents should be fetched in a single request?
  *
  *   If this value is too small, we have to make extra requests and
  *   the scroll will take longer.
  *
  *   If this value is too big, we may exceed the heap memory on a single
  *   request -- >100MB in one set of returned works, and we get an error:
  *
  *       org.apache.http.ContentTooLongException: entity content is too
  *       long [167209080] for the configured buffer limit [104857600]
  */
class ElasticsearchScanner()(implicit
                             client: ElasticClient,
                             keepAlive: FiniteDuration = 30 minutes,
                             bulkSize: Int = 10000)
    extends Logging {
  def scroll[T](
    request: SearchRequest
  )(implicit decoder: Decoder[T]): Iterator[T] =
    SearchIterator
      .hits(
        client,
        request
          .scroll(keepAlive)
          .size(bulkSize)
      )
      .zipWithIndex
      .map {
        // 1-index the documents for humans
        case (hit, index) => (hit, index + 1)
      }
      .map {
        case (hit, index) =>
          if (index % bulkSize == 0) {
            info(
              s"Received another ${intComma(bulkSize)} hits " +
                s"(${intComma(index)} so far) from ${request.indexes.string()}"
            )
          }

          hit
      }
      .map(_.safeTo[T].get)

  private def intComma(number: Long): String =
    NumberFormat.getInstance().format(number)
}
