package weco.api.search.works.filtering

class FilterAndAggregateByArchiveCategoryTest
    extends SingleFieldFilterTest("archive.category")
    with FilteringTestCases
    with AggregatingTestCases {

  // Each work belongs to exactly one archive category. Across the corpus:
  // GC has three works, PP has two, OH has one.
  val testWorks: Seq[String] = Seq(
    "works.archive.GC253.root",
    "works.archive.GC253.series",
    "works.archive.GC253.item",
    "works.archive.PPEBC.root",
    "works.archive.PPEBC.section",
    "works.archive.OH1.root"
  )

  val listingParams: String = "archive.category=GC"
  val listingResponse: String = worksListResponse(
    ids = Seq(
      "works.archive.GC253.root",
      "works.archive.GC253.series",
      "works.archive.GC253.item"
    )
  )

  val multipleParams: String = "archive.category=PP,OH"
  val multipleResponse: String = worksListResponse(
    ids = Seq(
      "works.archive.PPEBC.root",
      "works.archive.PPEBC.section",
      "works.archive.OH1.root"
    )
  )

  // "Correspondence" matches works in more than one category: the title of the
  // GC series, and both PP works (the title of the section, the description of
  // the root). The category filter narrows the search to the latter two.
  val searchingParams: String = "query=Correspondence&archive.category=PP"
  val searchingResponse: String = worksListResponse(
    ids = Seq(
      "works.archive.PPEBC.root",
      "works.archive.PPEBC.section"
    )
  )

  val allValuesParams: String =
    "archive.category=OH&aggregations=archive.category"
  val allValuesResponse: String = worksListResponseWithAggs(
    Seq("works.archive.OH1.root"),
    Map(
      "archive.category" -> Seq(
        (3, "GC", "General collections"),
        (2, "PP", "Personal papers"),
        (1, "OH", "Oral History")
      ).map {
        case (count, identifier, label) =>
          (count, s"""
               |{
               |            "id" : "$identifier",
               |            "label" : "$label"
               |          }
               |""".stripMargin)
      }
    )
  )

  val redundantFilterParams: String =
    "archive.category=GC&genres.label=NotAGenre&aggregations=archive.category"
  val redundantFilterBucket: String =
    """
      |{
      |            "id" : "GC",
      |            "label" : "General collections"
      |          }
      |""".stripMargin

  val aggregationName: String = "archive.category"
  val unattestedValueParams: String =
    "archive.category=xyz&genres.label=NotAGenre&aggregations=archive.category"
  val bogusValueResponse: String = ""
}
