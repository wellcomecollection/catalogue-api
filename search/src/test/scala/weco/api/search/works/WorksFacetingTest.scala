package weco.api.search.works

import akka.http.scaladsl.model.{ContentTypes, StatusCode}
import akka.http.scaladsl.server.Route
import io.circe.Json
import io.circe.parser.parse
import org.scalatest.GivenWhenThen
import weco.api.search.FacetingFeatures
import weco.fixtures.TestWith

class WorksFacetingTest
    extends FacetingFeatures
    with ApiWorksTestBase
    with GivenWhenThen {

  protected val resourcePath: String = s"$rootPath/works"

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

  private val marathiWorkTypeBuckets = Seq(
    Seq(
      (1, "a", "Books"),
      (1, "d", "Journals")
    ) map {
      case (count, code, label) =>
        toKeywordBucket("Format", count, code, label)
    }: _*
  )
  private val bookLanguagesBuckets = Seq(
    Seq(
      (3, "bak", "Bashkir"),
      (1, "mar", "Marathi")
    ) map {
      case (count, code, label) =>
        toKeywordBucket("Language", count, code, label)
    }: _*
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

    def failToGet(path: String): StatusCode = eventually {
      Get(path) ~> route ~> check {
        status shouldNot equal(Status.OK)
        status
      }
    }
  }
  override protected def Given[R](msg: String)(
    testWith: TestWith[JsonServer, R]
  ): R = {
    super[GivenWhenThen].Given(msg)
    withFacetedAPI[R] {
      testWith(_)
    }
  }

  protected def withFacetedAPI[R](testWith: TestWith[JsonServer, R]): R =
    withWorksApi[R] {
      case (worksIndex, route) =>
        indexTestDocuments(worksIndex, aggregatedWorks: _*)
        testWith(new WorksJsonServer(route))
    }

  protected val oneAggregation: ScenarioData = ScenarioData(
    aggregationFields = Seq("workType"),
    expectedAggregationBuckets = Map("workType" -> workTypeBuckets)
  )

  protected val twoAggregations: ScenarioData = ScenarioData(
    aggregationFields = Seq("workType", "languages"),
    expectedAggregationBuckets = Map(
      "workType" -> workTypeBuckets,
      "languages" -> languageBuckets
    )
  )

  protected val queryAndAggregations: ScenarioData = ScenarioData(
    queryTerm = Some("capybara"),
    aggregationFields = Seq("workType", "languages"),
    expectedAggregationBuckets = Map(
      "workType" -> capybaraWorkTypeBuckets,
      "languages" -> capybaraLanguageBuckets
    )
  )

  protected val filterOneAggregateAnother: ScenarioData = ScenarioData(
    aggregationFields = Seq("workType"),
    filters = Seq(("languages", "mar")),
    expectedAggregationBuckets = Map(
      "workType" -> marathiWorkTypeBuckets
    )
  )

  protected val filterAndAggregateSame: ScenarioData = ScenarioData(
    aggregationFields = Seq("workType"),
    filters = Seq(("workType", "a")),
    expectedAggregationBuckets = Map(
      "workType" -> workTypeBuckets
    )
  )

  protected val filterMultiAndAggregateSame: ScenarioData = ScenarioData(
    aggregationFields = Seq("workType"),
    filters = Seq(("workType", "a"), ("workType", "d")),
    expectedAggregationBuckets = Map(
      "workType" -> workTypeBuckets
    )
  )

  protected val filterAndAggregateMultiFields: ScenarioData = ScenarioData(
    aggregationFields = Seq("workType", "languages"),
    filters = Seq(("workType", "a"), ("languages", "mar")),
    expectedAggregationBuckets = Map(
      "workType" -> marathiWorkTypeBuckets,
      "languages" -> bookLanguagesBuckets
    )
  )

  protected val mutexFilter: ScenarioData = ScenarioData(
    aggregationFields = Seq("workType", "languages"),
    filters = Seq(("workType", "k"), ("languages", "mar")),
    expectedAggregationBuckets = Map(
      "workType" -> (marathiWorkTypeBuckets :+ toKeywordBucket(
        "Format",
        0,
        "k",
        "Pictures"
      )),
      "languages" -> Seq(
        toKeywordBucket("Language", 1, "que", "Quechua"),
        toKeywordBucket("Language", 0, "mar", "Marathi")
      )
    )
  )

  protected val emptyBucketFilter: ScenarioData = ScenarioData(
    aggregationFields = Seq("subjects.label"),
    expectedAggregationBuckets = Map(
      "subjects.label" -> Nil
    )
  )

  protected val queryAndFilter: ScenarioData = ScenarioData(
    queryTerm = Some("tapirs"),
    aggregationFields = Seq("workType"),
    filters = Seq(("languages", "que")),
    // tapirs alone would be Pictures(1), Books(1), Journals(1)
    // Quechua alone would be Pictures(1), Journals(2)
    expectedAggregationBuckets = Map(
      "workType" -> Seq(
        toKeywordBucket("Format", 1, "k", "Pictures"),
        toKeywordBucket("Format", 1, "d", "Journals")
      )
    )
  )
}
