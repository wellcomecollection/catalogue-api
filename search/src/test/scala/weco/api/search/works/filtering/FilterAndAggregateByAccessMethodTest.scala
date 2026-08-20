package weco.api.search.works.filtering

class FilterAndAggregateByAccessMethodTest
    extends SingleFieldFilterTest("access method")
    with FilteringTestCases
    with AggregatingTestCases {
  val testWorks = Seq(
    "works.examples.availabilities.online-only",
    "works.examples.availabilities.everywhere",
    "works.examples.access-status-filters-tests.0",
    "works.examples.access-status-filters-tests.1"
  )

  val listingParams: String =
    "items.locations.accessConditions.method.id=view-online"
  val listingResponse: String = worksListResponse(
    ids = Seq(
      "works.examples.availabilities.online-only",
      "works.examples.availabilities.everywhere"
    )
  )
  val multipleParams: String =
    "items.locations.accessConditions.method.id=view-online,manual-request"
  val multipleResponse: String = worksListResponse(ids = testWorks)
  val searchingParams: String =
    "query=nowhere&items.locations.accessConditions.method.id=view-online"
  val searchingResponse: String = worksListResponse(ids = Nil)

  val aggregationName: String = "items.locations.accessConditions.method"
  val allValuesParams: String =
    "items.locations.accessConditions.method.id=view-online&aggregations=items.locations.accessConditions.method"
  val allValuesResponse: String = worksListResponseWithAggs(
    Seq(
      "works.examples.availabilities.online-only",
      "works.examples.availabilities.everywhere"
    ),
    Map(
      "items.locations.accessConditions.method" -> Seq(
        (2, "manual-request", "Manual request"),
        (2, "view-online", "View online")
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
    "items.locations.accessConditions.method.id=view-online&genres.label=ThisIsNotAGenre&aggregations=items.locations.accessConditions.method"
  val redundantFilterBucket: String = """{
                                       |            "id" : "view-online",
                                       |            "label" : "View online"
                                       |          }""".stripMargin
  val unattestedValueParams: String =
    "items.locations.accessConditions.method.id=open-shelves&genres.label=ThisIsNotAGenre&aggregations=items.locations.accessConditions.method"
}
