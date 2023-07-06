package weco.api.search.works.filtering

import org.scalatest.funspec.AnyFunSpec
import weco.api.search.works.ApiWorksTestBase

trait AggregatingTestCases extends AnyFunSpec with ApiWorksTestBase {

  this: SingleFieldFilterTest =>

  val testWorks: Seq[String]
  val aggregationName: String

  val allValuesParams: String
  val allValuesResponse: String

  val redundantFilterParams: String
  val redundantFilterBucket: String

  val bogusValueParams: String

  describe(s"filtering and aggregating on $fieldName") {
    it(
      s"returns an aggregation over all values in $fieldName when filtering by $fieldName"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, testWorks: _*)
          assertJsonResponse(
            routes,
            path = s"$rootPath/works?$allValuesParams"
          ) {
            Status.OK -> allValuesResponse
          }
      }
    }

    it(
      "does not return an aggregation containing the filtered value if the value is bogus"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, testWorks: _*)
          assertJsonResponse(
            routes,
            path = s"$rootPath/works?$bogusValueParams"
          ) {
            Status.OK -> worksListResponseWithAggs(
              Nil,
              Map(
                aggregationName -> Nil
              )
            )
          }
      }

    }

    it(
      "returns an aggregation containing the filtered value even when the bucket count is zero"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, testWorks: _*)
          assertJsonResponse(
            routes,
            path = s"$rootPath/works?$redundantFilterParams"
          ) {
            Status.OK -> worksListResponseWithAggs(
              Nil,
              Map(
                aggregationName -> Seq(
                  (0, redundantFilterBucket)
                )
              )
            )
          }
      }
    }
  }

}
