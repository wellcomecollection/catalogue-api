package weco.api.search.images

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.server.Route
import io.circe.Json
import io.circe.parser.parse

import weco.api.search.FacetingFeatures
import weco.fixtures.TestWith

class ImagesFacetingTest extends FacetingFeatures with ApiImagesTestBase {

  protected val resourcePath: String = s"$rootPath/images"

  private val aggregatedImages =
    (0 to 6).map(i => s"images.different-licenses.$i")

  private class ImagesJsonServer(route: Route) extends JsonServer {
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
    withImagesApi[R] {
      case (worksIndex, route) =>
        indexTestDocuments(worksIndex, aggregatedImages: _*)
        testWith(new ImagesJsonServer(route))
    }

  val licenceBuckets: Seq[Json] = Seq(
    """
      |{
      |    "data": {
      |      "id": "cc-by",
      |      "label": "Attribution 4.0 International (CC BY 4.0)",
      |      "type": "License",
      |      "url": "http://creativecommons.org/licenses/by/4.0/"
      |    }
      |    ,
      |    "count": 5
      |    ,
      |    "type": "AggregationBucket"
      |  }
      |""".stripMargin,
    """{
        |    "data": {
        |      "id": "pdm",
        |      "label": "Public Domain Mark",
        |      "type": "License",
        |      "url": "https://creativecommons.org/share-your-work/public-domain/pdm/"
        |    }
        |    ,
        |    "count": 2
        |    ,
        |    "type": "AggregationBucket"
        |  }""".stripMargin
  ).map(parse(_).right.get)

  protected val oneAggregation: ScenarioData = ScenarioData(
    aggregationFields = Seq("locations.license"),
    expectedAggregationBuckets = Map("license" -> licenceBuckets)
  )

  protected val twoAggregations: ScenarioData = ScenarioData()
  protected val queryAndAggregations: ScenarioData = ScenarioData()
  protected val filterOneAggregateAnother: ScenarioData = ScenarioData()
  protected val filterAndAggregateSame: ScenarioData = ScenarioData()
  protected val filterMultiAndAggregateSame: ScenarioData = ScenarioData()

}
