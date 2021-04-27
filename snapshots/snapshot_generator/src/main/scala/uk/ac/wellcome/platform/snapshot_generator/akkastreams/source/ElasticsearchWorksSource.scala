package uk.ac.wellcome.platform.snapshot_generator.akkastreams.source

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.sksamuel.elastic4s.{ElasticClient, Index}
import com.sksamuel.elastic4s.ElasticDsl.{search, termQuery}
import com.sksamuel.elastic4s.requests.searches.{SearchHit, SearchIterator}
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil.fromJson
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.platform.snapshot_generator.models.SnapshotGeneratorConfig
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed

import java.text.NumberFormat
import scala.concurrent.duration._

class ElasticsearchHits(
  implicit
  client: ElasticClient,
  timeout: FiniteDuration
) extends Logging {
  def scroll(index: Index): Iterator[SearchHit] =
    SearchIterator
      .hits(
        client, search(index)
          .query(termQuery("type", "Visible"))
          .scroll(timeout)
      )
      .zipWithIndex
      .map { case (hit, index) =>
        if (index % 10000 == 0) {
          info(s"Received another ${intComma(10000)} hits " +
            s"(${intComma(index)} so far) from $index")
        }
        
        hit
      }

  private def intComma(number: Long): String =
    NumberFormat.getInstance().format(number)
}

object ElasticsearchWorksSource extends Logging {
  def apply(elasticClient: ElasticClient,
            snapshotConfig: SnapshotGeneratorConfig): Source[Work[Indexed], NotUsed] = {
    val iterator = new ElasticsearchHits()(
      elasticClient,
      5 minutes
    )

    Source
      .fromIterator(() => iterator.scroll(snapshotConfig.index))
      .map { searchHit: SearchHit =>
        fromJson[Work[Indexed]](searchHit.sourceAsString).get
      }
  }
}
