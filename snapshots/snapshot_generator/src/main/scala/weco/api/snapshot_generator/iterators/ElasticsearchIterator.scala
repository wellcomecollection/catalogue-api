package weco.api.snapshot_generator.iterators

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.circe._
import grizzled.slf4j.Logging
import io.circe.Json
import weco.api.search.elasticsearch.ResilientElasticClient
import weco.json.JsonUtil._

import scala.concurrent.Await
import scala.concurrent.duration._

class ElasticsearchIterator(implicit
                            client: ResilientElasticClient,
                            keepAlive: FiniteDuration = 30 minutes)
    extends Logging {
  case class HasDisplay(display: Json)

  def scroll(
    index: Index,
    bulkSize: Int,
    query: Option[String]
  ): Iterator[String] = {
    val searchReq = search(index)
      .rawQuery(query.getOrElse(matchAll))
      .sourceInclude("display")
      .scroll(keepAlive)
      .size(bulkSize)

    val initialResponse = Await.result(client.execute(searchReq), Duration.Inf)

    if (initialResponse.isError) throw initialResponse.error.asException

    new Iterator[String] {
      private var scrollId = initialResponse.result.scrollId
      private var hits = initialResponse.result.hits.hits
      private var currentIdx = 0

      override def hasNext: Boolean =
        if (currentIdx < hits.length) true
        else if (scrollId.isEmpty) false
        else {
          fetchNextBatch()
          currentIdx < hits.length
        }

      override def next(): String = {
        if (!hasNext) throw new NoSuchElementException("next on empty iterator")
        val hit = hits(currentIdx)
        currentIdx += 1
        // We use safeTo[HasDisplay] which relies on the implicit decoder from JsonUtil
        hit.safeTo[HasDisplay].map(_.display.noSpaces).get
      }

      private def fetchNextBatch(): Unit =
        scrollId.foreach { id =>
          val scrollReq = searchScroll(id).keepAlive(keepAlive)
          val response = Await.result(client.execute(scrollReq), Duration.Inf)
          if (response.isError) throw response.error.asException

          val result = response.result
          scrollId = result.scrollId
          hits = result.hits.hits
          currentIdx = 0

          if (hits.isEmpty) {
            scrollId = None
          }
        }
    }
  }

  private val matchAll = """{ "match_all": {} }"""
}
