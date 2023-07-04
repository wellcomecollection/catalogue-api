package weco.api.search.works.filtering

import org.scalatest.funspec.AnyFunSpec
import weco.api.search.works.ApiWorksTestBase

/**
  * Test cases that cover filtering by simple values such as ids.
  */
trait FilteringTestCases extends AnyFunSpec with ApiWorksTestBase {
  val testWorks: Seq[String]

  val listingParams: String
  val listingResponse: String

  val multipleParams: String
  val multipleResponse: String

  val searchingParams: String
  val searchingResponse: String

  describe(s"filtering by simple values") {
    it(s"filters by one value when listing works") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, testWorks: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?$listingParams") {
            Status.OK -> listingResponse
          }
      }
    }

    it(s"filters by multiple values when listing works") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, testWorks: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?$multipleParams"
          ) {
            Status.OK -> multipleResponse
          }
      }
    }

    it("when searching for works") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, testWorks: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?$searchingParams"
          ) {
            Status.OK -> searchingResponse
          }
      }
    }
  }

}
