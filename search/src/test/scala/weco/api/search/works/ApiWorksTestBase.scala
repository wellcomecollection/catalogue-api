package weco.api.search.works

import io.circe.Json
import org.scalatest.Suite
import weco.api.search.ApiTestBase
import weco.api.search.fixtures.TestDocumentFixtures
import weco.api.search.json.CatalogueJsonUtil
import weco.api.search.models.request.WorksIncludes

trait ApiWorksTestBase
    extends ApiTestBase
    with CatalogueJsonUtil
    with TestDocumentFixtures {
  this: Suite =>

  def getMinimalDisplayWorks(ids: Seq[String]): Seq[Json] =
    ids
      .map { getVisibleWork }
      .map(_.display.withIncludes(WorksIncludes.none))
      .sortBy(w => getKey(w, "id").get.asString)

  def worksListResponse(
    ids: Seq[String],
    includes: WorksIncludes = WorksIncludes.none,
    strictOrdering: Boolean = false
  ): String =
    s"{${worksList(ids, includes, strictOrdering)}}"

  private def worksList(
    ids: Seq[String],
    includes: WorksIncludes = WorksIncludes.none,
    strictOrdering: Boolean = false
  ): String = {
    val works =
      ids
        .map {
          getVisibleWork
        }
        .map(_.display.withIncludes(includes))

    val sortedWorks = if (strictOrdering) {
      works
    } else {
      works.sortBy(w => getKey(w, "id").get.asString)
    }
    s"""
       |  ${resultListWithCalculatedPageCount(
         totalResults = ids.size
       )},
       |  "results": [
       |    ${sortedWorks.mkString(",")}
       |  ]
      """.stripMargin
  }

  def worksListResponseWithAggs(
    ids: Seq[String],
    aggs: Map[String, Seq[(Int, String)]]
  ) =
    s"{${worksList(ids)}, ${aggregations(aggs)}}"

  private def aggregations(aggs: Map[String, Seq[(Int, String)]]): String = {
    val aggregationEntries = aggs map {
      case (key, buckets) =>
        val aggregationBuckets = buckets map {
          case (count, bucketData) =>
            s"""
               |{
               |          "count" : $count,
               |          "data" : $bucketData,
               |          "type" : "AggregationBucket"
               |        }
               |""".stripMargin

        }
        s""""$key": {"buckets": [${aggregationBuckets.mkString(",")}], "type" : "Aggregation"}"""
    }
    s""" "aggregations":{${aggregationEntries.mkString(",")}, "type" : "Aggregations"}"""

  }
}
