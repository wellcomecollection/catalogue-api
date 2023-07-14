package weco.api.search.works.filtering

import org.scalatest.prop.TableFor3

class FilterAndAggregateByContributorTest
    extends SingleFieldFilterTest("contributor")
    with FreeTextFilteringTestCases
    with AggregatingTestCases {

  val patriciaWork = "works.examples.contributor-filters-tests.0"
  val karlMarxWork = "works.examples.contributor-filters-tests.1"
  val jakePaulWork = "works.examples.contributor-filters-tests.2"
  val darwinWork = "works.examples.contributor-filters-tests.3"
  val patriciaDarwinWork = "works.examples.contributor-filters-tests.4"
  val noContributorsWork = "works.examples.contributor-filters-tests.5"

  val testWorks = Seq(
    patriciaWork,
    karlMarxWork,
    jakePaulWork,
    darwinWork,
    patriciaDarwinWork,
    noContributorsWork
  )

  val filterName: String = "contributors.agent.label"

  protected val freeTextExamples: TableFor3[String, Seq[String], String] =
    Table(
      ("query", "expectedIds", "clue"),
      ("Karl Marx", Seq(karlMarxWork), "single match"),
      (
        """"Bath, Patricia"""",
        Seq(patriciaWork, patriciaDarwinWork),
        "multi match"
      ),
      (
        "Karl Marx,Jake Paul",
        Seq(karlMarxWork, jakePaulWork),
        "comma separated"
      ),
      (
        """"Bath, Patricia",Karl Marx""",
        Seq(patriciaWork, patriciaDarwinWork, karlMarxWork),
        "commas in quotes"
      ),
      (
        """"Bath, Patricia",Karl Marx,"Darwin \"Jones\", Charles"""",
        Seq(patriciaWork, karlMarxWork, darwinWork, patriciaDarwinWork),
        "quotes in quotes"
      )
    )

  val allValuesParams: String =
    "contributors.agent.label=Karl%20Marx&aggregations=contributors.agent.label"
  val allValuesResponse: String = worksListResponseWithAggs(
    Seq(karlMarxWork),
    Map(
      "contributors.agent.label" -> Seq(
        (2, "Bath, Patricia"),
        (2, "Darwin \\\"Jones\\\", Charles"),
        (1, "Karl Marx"),
        (1, "Jake Paul")
      ).map {
        case (count, label) =>
          (count, s"""
               |{
     |            "label" : "$label",
     |            "type" : "Person"
     |          }
               |""".stripMargin)
      }
    )
  )
  val redundantFilterParams: String =
    s"contributors.agent.label=Karl%20Marx&genres.label=NotAGenre&aggregations=contributors.agent.label"
  val redundantFilterBucket: String =
    """
      |{
      |            "label" : "Karl Marx",
      |            "type" : "Person"
      |          }
      |""".stripMargin

  val aggregationName: String = "contributors.agent.label"
  val bogusValueParams: String =
    "languages=sjn&contributors.agent.label=Joseph%20Pujol&aggregations=contributors.agent.label"
  val bogusValueResponse: String = ""
}
