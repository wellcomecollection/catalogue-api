package weco.api.snapshot_generator.iterators

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl.{search, termQuery}
import grizzled.slf4j.Logging
import io.circe.Json
import weco.api.snapshot_generator.models.SnapshotGeneratorConfig
import weco.elasticsearch.ElasticsearchScanner
import weco.json.JsonUtil._

import scala.concurrent.duration._

class ElasticsearchIterator(
  implicit
  client: ElasticClient,
  timeout: FiniteDuration = 5 minutes
) extends Logging {
  case class HasDisplay(display: Json)

  def scroll(
    config: SnapshotGeneratorConfig
  ): Iterator[String] = {
    val underlying = new ElasticsearchScanner()(
      client,
      timeout = timeout,
      bulkSize = config.bulkSize
    )

    underlying
      .scroll[HasDisplay](
        search(config.index)
          .query(termQuery("type", "Visible"))
          .sourceInclude("display")
      )
      .map(_.display.noSpaces)
  }
}
