package weco.api.search.works.filtering

class FilterAndAggregateByCollectionRootTest
    extends SingleFieldFilterTest("collectionRoot")
    with FilteringTestCases
    with AggregatingTestCases {
  val testWorks: Seq[String] = Seq(
    "works.collection-root.0.aca",
    "works.collection-root.1.aca",
    "works.collection-root.2.aca",
    "works.collection-root.3.aca+bca",
    "works.collection-root.4.aca+bca+cca",
    "works.collection-root.5.bca",
    "works.collection-root.6.cca"
  )
  val listingParams: String = "collectionRoot=aca"
  val listingResponse: String = worksListResponse(
    ids = Seq(
      "works.collection-root.0.aca",
      "works.collection-root.1.aca",
      "works.collection-root.2.aca",
      "works.collection-root.3.aca+bca",
      "works.collection-root.4.aca+bca+cca"
    )
  )

  val multipleParams: String = "collectionRoot=bca,cca"
  val multipleResponse: String = worksListResponse(
    ids = Seq(
      "works.collection-root.3.aca+bca",
      "works.collection-root.4.aca+bca+cca",
      "works.collection-root.5.bca",
      "works.collection-root.6.cca"
    )
  )

  val searchingParams: String = "query=Business&collectionRoot=cca"
  val searchingResponse: String = worksListResponse(
    ids = Seq(
      "works.collection-root.4.aca+bca+cca"
    )
  )

  val allValuesParams: String = "collectionRoot=cca&aggregations=collectionRoot"
  val allValuesResponse: String = worksListResponseWithAggs(
    Seq("works.collection-root.4.aca+bca+cca", "works.collection-root.6.cca"),
    Map(
      "collectionRoot" -> Seq(
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
    "collectionRoot=aca&genres.label=NotAGenre&aggregations=collectionRoot"
  val redundantFilterBucket: String =
    """
      |{
      |            "id" : "aca",
      |            "label" : "Wellcome Archive"
      |          }
      |""".stripMargin
  val aggregationName: String = "collectionRoot"
  val unattestedValueParams: String =
    "collectionRoot=xyz&genres.label=NotAGenre&aggregations=collectionRoot"
  val bogusValueResponse: String = ""
}
