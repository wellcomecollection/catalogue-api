package weco.api.snapshot_generator.iterators

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl.{search, termQuery}
import grizzled.slf4j.Logging
import weco.api.snapshot_generator.models.SnapshotGeneratorConfig
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed
import weco.elasticsearch.ElasticsearchScanner

import scala.concurrent.duration._

class ElasticsearchWorksIterator(
  implicit
  client: ElasticClient,
  timeout: FiniteDuration = 5 minutes
) extends Logging {
  def scroll(config: SnapshotGeneratorConfig): Iterator[Work.Visible[Indexed]] = {
    val underlying = new ElasticsearchScanner()(
      client, timeout = timeout, bulkSize = config.bulkSize
    )

    underlying
      .scroll[Work.Visible[Indexed]](
        search(config.index)
          .query(termQuery("type", "Visible"))
      )
  }
}
