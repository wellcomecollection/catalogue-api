package weco.api.snapshot_generator.iterators

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl.{search, termQuery}
import grizzled.slf4j.Logging
import weco.api.search.models.index.IndexedWork
import weco.api.snapshot_generator.models.SnapshotGeneratorConfig
import weco.elasticsearch.ElasticsearchScanner
import weco.json.JsonUtil._

import scala.concurrent.duration._

class ElasticsearchWorksIterator(
  implicit
  client: ElasticClient,
  timeout: FiniteDuration = 5 minutes
) extends Logging {
  def scroll(
    config: SnapshotGeneratorConfig
  ): Iterator[String] = {
    val underlying = new ElasticsearchScanner()(
      client,
      timeout = timeout,
      bulkSize = config.bulkSize
    )

    underlying
      .scroll[IndexedWork.Visible](
        search(config.index)
          .query(termQuery("type", "Visible"))
      )
      .map(_.display.noSpaces)
  }
}
