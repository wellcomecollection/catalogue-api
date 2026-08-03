package weco.api.search.works.filtering

class FilterAndAggregateByArchiveTypeTest
    extends SingleFieldFilterTest("archiveType")
    with FilteringTestCases
    with AggregatingTestCases {
  val testWorks: Seq[String] = Seq(
    "works.archive-type.0.per",
    "works.archive-type.1.per",
    "works.archive-type.2.per",
    "works.archive-type.3.per+cor",
    "works.archive-type.4.per+cor+fam",
    "works.archive-type.5.cor",
    "works.archive-type.6.fam"
  )
  val listingParams: String = "archiveType=per"
  val listingResponse: String = worksListResponse(
    ids = Seq(
      "works.archive-type.0.per",
      "works.archive-type.1.per",
      "works.archive-type.2.per",
      "works.archive-type.3.per+cor",
      "works.archive-type.4.per+cor+fam"
    )
  )

  val multipleParams: String = "archiveType=cor,fam"
  val multipleResponse: String = worksListResponse(
    ids = Seq(
      "works.archive-type.3.per+cor",
      "works.archive-type.4.per+cor+fam",
      "works.archive-type.5.cor",
      "works.archive-type.6.fam"
    )
  )

  val searchingParams: String = "query=Corporate&archiveType=fam"
  val searchingResponse: String = worksListResponse(
    ids = Seq(
      "works.archive-type.4.per+cor+fam"
    )
  )

  val allValuesParams: String = "archiveType=fam&aggregations=archiveType"
  val allValuesResponse: String = worksListResponseWithAggs(
    Seq("works.archive-type.4.per+cor+fam", "works.archive-type.6.fam"),
    Map(
      "archiveType" -> Seq(
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
    "archiveType=per&genres.label=NotAGenre&aggregations=archiveType"
  val redundantFilterBucket: String =
    """
      |{
      |            "id" : "per",
      |            "label" : "Personal"
      |          }
      |""".stripMargin
  val aggregationName: String = "archiveType"
  val unattestedValueParams: String =
    "archiveType=xyz&genres.label=NotAGenre&aggregations=archiveType"
  val bogusValueResponse: String = ""
}
