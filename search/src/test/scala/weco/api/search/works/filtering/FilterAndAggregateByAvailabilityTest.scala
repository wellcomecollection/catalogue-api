package weco.api.search.works.filtering

class FilterAndAggregateByAvailabilityTest
    extends FilteringTestCases
    with AggregatingTestCases {
  val testWorks = Seq(
    "works.examples.availabilities.open-only",
    "works.examples.availabilities.closed-only",
    "works.examples.availabilities.online-only",
    "works.examples.availabilities.everywhere",
    "works.examples.availabilities.nowhere"
  )

  val listingParams: String = "availabilities=open-shelves"
  val listingResponse: String = worksListResponse(
    ids = Seq(
      "works.examples.availabilities.open-only",
      "works.examples.availabilities.everywhere"
    )
  )
  val multipleParams: String = "availabilities=open-shelves,closed-stores"
  val multipleResponse: String = worksListResponse(
    ids = Seq(
      "works.examples.availabilities.open-only",
      "works.examples.availabilities.everywhere",
      "works.examples.availabilities.closed-only"
    )
  )
  val searchingParams: String = "query=nowhere&availabilities=open-shelves"
  val searchingResponse: String = worksListResponse(ids = Nil)

  val aggregationName: String = "availabilities"
  val allValuesParams: String =
    "availabilities=open-shelves&aggregations=availabilities"
  val allValuesResponse: String = worksListResponseWithAggs(
    Seq(
      "works.examples.availabilities.open-only",
      "works.examples.availabilities.everywhere"
    ),
    Map(
      "availabilities" -> Seq(
        (2, "closed-stores", "Closed stores"),
        (2, "online", "Online"),
        (2, "open-shelves", "Open shelves")
      ).map {
        case (count, identifier, label) =>
          (count, s"""
               |{
               |            "id" : "$identifier",
               |            "label" : "$label",
               |            "type" : "Availability"
               |          }
               |""".stripMargin)
      }
    )
  )
  val redundantFilterParams: String =
    "availabilities=online&genres.label=ThisIsNotAGenre&aggregations=availabilities"
  val redundantFilterBucket: String = """{
                                       |            "id" : "online",
                                       |            "label" : "Online",
                                       |            "type" : "Availability"
                                       |          }""".stripMargin
  val bogusValueParams: String =
    "availabilities=on-the-moon&genres.label=ThisIsNotAGenre&aggregations=availabilities"
}
