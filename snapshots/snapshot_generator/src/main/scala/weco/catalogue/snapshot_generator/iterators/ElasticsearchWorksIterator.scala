package weco.catalogue.snapshot_generator.iterators

import com.sksamuel.elastic4s.ElasticDsl.{search, termQuery}
import com.sksamuel.elastic4s.requests.searches.{SearchHit, SearchIterator}
import com.sksamuel.elastic4s.ElasticClient
import grizzled.slf4j.Logging
import weco.json.JsonUtil._
import uk.ac.wellcome.models.Implicits._
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed
import weco.catalogue.snapshot_generator.models.SnapshotGeneratorConfig

import java.text.NumberFormat
import scala.concurrent.duration._

class ElasticsearchWorksIterator(
  implicit
  client: ElasticClient,
  timeout: FiniteDuration = 5 minutes
) extends Logging {
  def scroll(config: SnapshotGeneratorConfig): Iterator[Work.Visible[Indexed]] =
    SearchIterator
      .hits(
        client,
        search(config.index)
          .query(termQuery("type", "Visible"))
          .scroll(timeout)
          .size(config.bulkSize)
      )
      .zipWithIndex
      .map {
        case (hit, index) =>
          if (index % 10000 == 0) {
            info(
              s"Received another ${intComma(10000)} hits " +
                s"(${intComma(index)} so far) from $index")
          }

          hit
      }
      .map { searchHit: SearchHit =>
        fromJson[Work.Visible[Indexed]](searchHit.sourceAsString).get
      }

  private def intComma(number: Long): String =
    NumberFormat.getInstance().format(number)
}
