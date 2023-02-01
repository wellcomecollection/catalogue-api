package weco.api.snapshot_generator.iterators

import com.sksamuel.elastic4s.{ElasticClient, Index}
import com.sksamuel.elastic4s.ElasticDsl.search
import grizzled.slf4j.Logging
import io.circe.Json
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
    index: Index,
    bulkSize: Int,
    query: Option[String]
  ): Iterator[String] = {
    val underlying = new ElasticsearchScanner()(
      client,
      timeout = timeout,
      bulkSize = bulkSize
    )

    underlying
      .scroll[HasDisplay](
        search(index)
          .rawQuery(query.getOrElse(matchAll))
          .sourceInclude("display")
      )
      .map(_.display.noSpaces)
  }

  private val matchAll = """{ "match_all": {} }"""
}
