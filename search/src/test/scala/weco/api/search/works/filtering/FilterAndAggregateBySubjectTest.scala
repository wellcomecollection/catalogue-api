package weco.api.search.works.filtering

import org.scalatest.prop.TableFor3

import java.net.URLEncoder

class FilterAndAggregateBySubjectTest
    extends SingleFieldFilterTest("subject")
    with FreeTextFilteringTestCases
    with AggregatingTestCases {

  val sanitationWork = "works.examples.subject-filters-tests.0"
  val londonWork = "works.examples.subject-filters-tests.1"
  val psychologyWork = "works.examples.subject-filters-tests.2"
  val darwinWork = "works.examples.subject-filters-tests.3"
  val mostThingsWork = "works.examples.subject-filters-tests.4"
  val nothingWork = "works.examples.subject-filters-tests.5"

  val testWorks: Seq[String] =
    Seq(
      sanitationWork,
      londonWork,
      psychologyWork,
      darwinWork,
      mostThingsWork,
      nothingWork
    )

  val filterName: String = "subjects.label"

  protected val freeTextExamples: TableFor3[String, Seq[String], String] =
    Table(
      ("query", "expectedIds", "clue"),
      (
        "Sanitation.",
        Seq(sanitationWork),
        "single match single subject"
      ),
      (
        "London (England)",
        Seq(londonWork, mostThingsWork),
        "multi match single subject"
      ),
      (
        "Sanitation.,London (England)",
        Seq(sanitationWork, londonWork, mostThingsWork),
        "comma separated"
      ),
      (
        """Sanitation.,"Psychology, Pathological"""",
        Seq(sanitationWork, psychologyWork, mostThingsWork),
        "commas in quotes"
      ),
      (
        """"Darwin \"Jones\", Charles","Psychology, Pathological",London (England)""",
        Seq(darwinWork, psychologyWork, londonWork, mostThingsWork),
        "escaped quotes in quotes"
      )
    )

  val allValuesParams: String =
    "subjects.label=Sanitation.&aggregations=subjects.label"
  val allValuesResponse: String = worksListResponseWithAggs(
    Seq(sanitationWork),
    Map(
      "subjects.label" -> Seq(
        (2, "Darwin \\\"Jones\\\", Charles"),
        (2, "London (England)"),
        (2, "Psychology, Pathological"),
        (1, "Sanitation.")
      ).map {
        case (count, label) =>
          (count, s"""
               |{
               |  "concepts" : [
     |            ],
     |            "label" : "$label",
     |            "type" : "Subject"
     |          }
               |""".stripMargin)
      }
    )
  )
  val redundantFilterParams: String =
    s"subjects.label=${URLEncoder.encode(""""Psychology, Pathological"""", "UTF-8")}&genres.label=NotAGenre&aggregations=subjects.label"
  val redundantFilterBucket: String =
    """
      |{
      |           "concepts" : [
      |            ],
      |            "label" : "Psychology, Pathological",
      |            "type" : "Subject"
      |          }
      |""".stripMargin
  val aggregationName: String = "subjects.label"
  val bogusValueParams: String =
    "languages=sjn&subjects.label=NotASubject&aggregations=subjects.label"
  val bogusValueResponse: String = ""
}
