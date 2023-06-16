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
  protected val aggregableFields: Seq[String] = Seq("workType", "languages")
  protected val queries: Seq[String] = Seq("capybara")
  private def toKeywordBucket(
    dataType: String,
    count: Int,
    code: String,
    label: String
  ): Json =
    parse(s"""
         |{
         |"count": $count,
         |"data": {
         |  "id": "$code",
         |  "label": "$label",
         |  "type": "$dataType"
         |},
         |"type": "AggregationBucket"
         |}""".stripMargin).right.get

  private val workTypeBuckets = Seq(
    Seq(
      (4, "a", "Books"),
      (3, "d", "Journals"),
      (2, "i", "Audio"),
      (1, "k", "Pictures")
    ) map {
      case (count, code, label) => toKeywordBucket("Format", count, code, label)
    }: _*
  )
  private val languageBuckets = Seq(
    Seq(
      (4, "bak", "Bashkir"),
      (3, "que", "Quechua"),
      (2, "mar", "Marathi"),
      (1, "che", "Chechen")
    ) map {
      case (count, code, label) =>
        toKeywordBucket("Language", count, code, label)
    }: _*
  )
  private val capybaraWorkTypeBuckets = Seq(
    Seq(
      (2, "a", "Books"),
      (1, "d", "Journals")
    ) map {
      case (count, code, label) =>
        toKeywordBucket("Format", count, code, label)
    }: _*
  )

  private val capybaraLanguageBuckets = Seq(
    Seq(
      (2, "mar", "Marathi"),
      (1, "bak", "Bashkir")
    ) map {
      case (count, code, label) =>
        toKeywordBucket("Language", count, code, label)
    }: _*
  )

  protected val buckets: Map[String, Seq[Json]] = Map(
    "workType" -> workTypeBuckets,
    "languages" -> languageBuckets,
    "capybara/workType" -> capybaraWorkTypeBuckets,
    "capybara/languages" -> capybaraLanguageBuckets
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
