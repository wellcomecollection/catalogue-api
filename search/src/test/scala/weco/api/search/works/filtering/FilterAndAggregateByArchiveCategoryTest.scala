package weco.api.search.works.filtering

class FilterAndAggregateByArchiveCategoryTest
    extends SingleFieldFilterTest("archive.category")
    with FilteringTestCases
    with AggregatingTestCases {
  val testWorks: Seq[String] = Seq(
    "works.archive-category.0.per",
    "works.archive-category.1.per",
    "works.archive-category.2.per",
    "works.archive-category.3.per+cor",
    "works.archive-category.4.per+cor+fam",
    "works.archive-category.5.cor",
    "works.archive-category.6.fam"
  )
  val listingParams: String = "archive.category=per"
  val listingResponse: String = worksListResponse(
    ids = Seq(
      "works.archive-category.0.per",
      "works.archive-category.1.per",
      "works.archive-category.2.per",
      "works.archive-category.3.per+cor",
      "works.archive-category.4.per+cor+fam"
    )
  )

  val multipleParams: String = "archive.category=cor,fam"
  val multipleResponse: String = worksListResponse(
    ids = Seq(
      "works.archive-category.3.per+cor",
      "works.archive-category.4.per+cor+fam",
      "works.archive-category.5.cor",
      "works.archive-category.6.fam"
    )
  )

  val searchingParams: String = "query=Corporate&archive.category=fam"
  val searchingResponse: String = worksListResponse(
    ids = Seq(
      "works.archive-category.4.per+cor+fam"
    )
  )

  val allValuesParams: String = "archive.category=fam&aggregations=archive.category"
  val allValuesResponse: String = worksListResponseWithAggs(
    Seq("works.archive-category.4.per+cor+fam", "works.archive-category.6.fam"),
    Map(
      "archive.category" -> Seq(
        (5, "per", "Personal"),
        (3, "cor", "Corporate"),
        (2, "fam", "Family")
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
    "archive.category=per&genres.label=NotAGenre&aggregations=archive.category"
  val redundantFilterBucket: String =
    """
      |{
      |            "id" : "per",
      |            "label" : "Personal"
      |          }
      |""".stripMargin
  val aggregationName: String = "archive.category"
  val unattestedValueParams: String =
    "archive.category=xyz&genres.label=NotAGenre&aggregations=archive.category"
  val bogusValueResponse: String = ""
}
