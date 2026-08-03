package weco.api.search.works.filtering

class FilterAndAggregateByArchiveRootTest
    extends SingleFieldFilterTest("archiveRoot")
    with FilteringTestCases
    with AggregatingTestCases {
  val testWorks: Seq[String] = Seq(
    "works.archive-root.0.aca",
    "works.archive-root.1.aca",
    "works.archive-root.2.aca",
    "works.archive-root.3.aca+bca",
    "works.archive-root.4.aca+bca+cca",
    "works.archive-root.5.bca",
    "works.archive-root.6.cca"
  )
  val listingParams: String = "archiveRoot=aca"
  val listingResponse: String = worksListResponse(
    ids = Seq(
      "works.archive-root.0.aca",
      "works.archive-root.1.aca",
      "works.archive-root.2.aca",
      "works.archive-root.3.aca+bca",
      "works.archive-root.4.aca+bca+cca"
    )
  )

  val multipleParams: String = "archiveRoot=bca,cca"
  val multipleResponse: String = worksListResponse(
    ids = Seq(
      "works.archive-root.3.aca+bca",
      "works.archive-root.4.aca+bca+cca",
      "works.archive-root.5.bca",
      "works.archive-root.6.cca"
    )
  )

  val searchingParams: String = "query=Business&archiveRoot=cca"
  val searchingResponse: String = worksListResponse(
    ids = Seq(
      "works.archive-root.4.aca+bca+cca"
    )
  )

  val allValuesParams: String = "archiveRoot=cca&aggregations=archiveRoot"
  val allValuesResponse: String = worksListResponseWithAggs(
    Seq("works.archive-root.4.aca+bca+cca", "works.archive-root.6.cca"),
    Map(
      "archiveRoot" -> Seq(
        (5, "aca", "Wellcome Archive"),
        (3, "bca", "Business Archive"),
        (2, "cca", "Charity Archive")
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
    "archiveRoot=aca&genres.label=NotAGenre&aggregations=archiveRoot"
  val redundantFilterBucket: String =
    """
      |{
      |            "id" : "aca",
      |            "label" : "Wellcome Archive"
      |          }
      |""".stripMargin
  val aggregationName: String = "archiveRoot"
  val unattestedValueParams: String =
    "archiveRoot=xyz&genres.label=NotAGenre&aggregations=archiveRoot"
  val bogusValueResponse: String = ""
}
