package weco.api.search.works.filtering

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3}
import weco.api.search.works.ApiWorksTestBase

import java.net.URLEncoder

/**
  * Test cases that cover filtering by complex values such as labels.
  * Filter terms on labels are likely to have tricky characters such as commas and quotes that
  * a naive processor might misinterpret as delimiters, rather than pass on to the database as
  * part of the filter.
  *
  */
trait FreeTextFilteringTestCases
    extends AnyFunSpec
    with ApiWorksTestBase
    with TableDrivenPropertyChecks {

  this: SingleFieldFilterTest =>

  val testWorks: Seq[String]
  val filterName: String

  protected val freeTextExamples: TableFor3[String, Seq[String], String]

  describe(s"filtering $fieldName by various values") {
    it(s"filters using a comma separated list") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, testWorks: _*)

          forAll(freeTextExamples) {
            (query: String, expectedIds: Seq[String], clue: String) =>
              withClue(clue) {
                assertJsonResponse(
                  routes,
                  path = s"$rootPath/works?$filterName=${URLEncoder
                    .encode(query, "UTF-8")}"
                ) {
                  Status.OK -> worksListResponse(expectedIds)
                }
              }
          }
      }
    }
  }
}
