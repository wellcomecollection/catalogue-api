package weco.api.search.works

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.server.Route
import io.circe.Json
import io.circe.parser.parse
import org.scalatest.featurespec.AnyFeatureSpec
import weco.api.search.FacetingFeatures
import weco.fixtures.TestWith

class WorksFacetingTest
    extends AnyFeatureSpec
    with FacetingFeatures
    with ApiWorksTestBase {

  protected val resourcePath: String = s"$rootPath/works"
  protected val singleAggregableField = "workType"
  private val workTypeBuckets = Json.arr(
    Seq(
      (4, "a", "Books"),
      (3, "d", "Journals"),
      (2, "i", "Audio"),
      (1, "k", "Pictures")
    ) map {
      case (count, code, label) =>
        parse(s"""
         |{
         |"count": $count,
         |"data": {
         |  "id": "$code",
         |  "label": "$label",
         |  "type": "Format"
         |},
         |"type": "AggregationBucket"
         |}""".stripMargin).right.get
    }: _*
  )

  protected val buckets: Map[String, Json] = Map(
    singleAggregableField -> workTypeBuckets
  )

  private val aggregatedWorks =
    (0 to 9).map(i => s"works.examples.filtered-aggregations-tests.$i")

  private class WorksJsonServer(route: Route) extends JsonServer {
    def getJson(path: String): Json =
      eventually {
        Get(path) ~> route ~> check {
          contentType shouldEqual ContentTypes.`application/json`
          status shouldEqual Status.OK
          parseJson(responseAs[String])
        }
      }
  }

  protected def withFacetedAPI[R](testWith: TestWith[JsonServer, R]): R =
    withWorksApi[R] {
      case (worksIndex, route) =>
        indexTestDocuments(worksIndex, aggregatedWorks: _*)
        testWith(new WorksJsonServer(route))
    }
}
