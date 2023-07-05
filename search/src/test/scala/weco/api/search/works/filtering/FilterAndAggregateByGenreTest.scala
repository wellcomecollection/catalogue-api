package weco.api.search.works.filtering

import io.circe.syntax.EncoderOps
import org.scalatest.prop.TableFor3

class FilterAndAggregateByGenreTest
    extends SingleFieldFilterTest("genre")
    with FreeTextFilteringTestCases
    with AggregatingTestCases {
  val annualReportsWork = s"works.examples.genre-filters-tests.0"
  val pamphletsWork = "works.examples.genre-filters-tests.1"
  val psychologyWork = "works.examples.genre-filters-tests.2"
  val darwinWork = "works.examples.genre-filters-tests.3"
  val mostThingsWork = "works.examples.genre-filters-tests.4"
  val nothingWork = "works.examples.genre-filters-tests.5"

  val testWorks: Seq[String] =
    Seq(
      annualReportsWork,
      pamphletsWork,
      psychologyWork,
      darwinWork,
      mostThingsWork,
      nothingWork
    )

  val freeTextExamples: TableFor3[String, Seq[String], String] = Table(
    ("query", "expectedIds", "clue"),
    ("Annual reports.", Seq(annualReportsWork), "single match single genre"),
    (
      "Pamphlets.",
      Seq(pamphletsWork, mostThingsWork),
      "multi match single genre"
    ),
    (
      "Annual reports.,Pamphlets.",
      Seq(annualReportsWork, pamphletsWork, mostThingsWork),
      "comma separated"
    ),
    (
      """Annual reports.,"Psychology, Pathological"""",
      Seq(annualReportsWork, psychologyWork, mostThingsWork),
      "commas in quotes"
    ),
    (
      """"Darwin \"Jones\", Charles","Psychology, Pathological",Pamphlets.""",
      Seq(darwinWork, psychologyWork, mostThingsWork, pamphletsWork),
      "escaped quotes in quotes"
    )
  )

  val filterName: String = "genres.label"

  val allValuesParams: String =
    "genres.label=Annual%20reports.&aggregations=genres.label"
  val allValuesResponse: String = worksListResponseWithAggs(
    Seq(annualReportsWork),
    Map(
      "genres.label" -> Seq(
        (2, "Darwin \"Jones\", Charles"),
        (2, "Pamphlets."),
        (2, "Psychology, Pathological"),
        (1, "Annual reports.")
      ).map {
        case (count, label) =>
          (count, s"""
               |{
               |            "concepts" : [
               |            ],
               |            "label" : ${label.asJson},
               |            "type" : "Genre"
               |          }
               |""".stripMargin)
      }
    )
  )

  val redundantFilterParams: String =
    s"genres.label=Pamphlets.&languages=sjn&aggregations=genres.label"
  val redundantFilterBucket: String =
    """
      |{
      |            "concepts" : [
      |            ],
      |            "label" : "Pamphlets.",
      |            "type" : "Genre"
      |          }
      |""".stripMargin
  val aggregationName: String = "genres.label"
  val bogusValueParams: String =
    "languages=sjn&genres.label=NotAGenre&aggregations=genres.label"
  val bogusValueResponse: String = ""
}
