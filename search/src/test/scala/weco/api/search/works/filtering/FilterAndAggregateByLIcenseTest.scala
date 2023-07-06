package weco.api.search.works.filtering

class FilterAndAggregateByLIcenseTest
    extends SingleFieldFilterTest("license")
    with FilteringTestCases
    with AggregatingTestCases {

  val testWorks = worksLicensed

  val listingParams: String = "items.locations.license=cc-by"
  val listingResponse: String = worksListResponse(
    ids = Seq(
      "works.items-with-licenses.0",
      "works.items-with-licenses.1",
      "works.items-with-licenses.3"
    )
  )
  val multipleParams: String = "items.locations.license=cc-by,cc-by-nc"
  val multipleResponse: String = worksListResponse(
    ids = Seq(
      "works.items-with-licenses.0",
      "works.items-with-licenses.1",
      "works.items-with-licenses.2",
      "works.items-with-licenses.3"
    )
  )

  //There is no good existing example for this.
  // When we next recreate the data, it will change, we should put a proper title in these example files
  val searchingParams: String =
    "query=sojyedta&items.locations.license=cc-by"
  val searchingResponse: String = worksListResponse(
    ids = Seq("works.items-with-licenses.1")
  )

  val aggregationName: String = "items.locations.license"
  val allValuesParams: String =
    "items.locations.license=cc-by-nc&aggregations=items.locations.license"
  val allValuesResponse: String = worksListResponseWithAggs(
    Seq(
      "works.items-with-licenses.2",
      "works.items-with-licenses.3"
    ),
    Map(
      "items.locations.license" -> Seq(
        (
          3,
          "cc-by",
          "Attribution 4.0 International (CC BY 4.0)",
          "http://creativecommons.org/licenses/by/4.0/"
        ),
        (
          2,
          "cc-by-nc",
          "Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)",
          "https://creativecommons.org/licenses/by-nc/4.0/"
        )
      ).map {
        case (count, identifier, label, url) =>
          (count, s"""
               |{
               |            "id" : "$identifier",
               |            "label" : "$label",
               |            "type" : "License",
               |            "url" : "$url"            
               |          }
               |""".stripMargin)
      }
    )
  )
  val redundantFilterParams: String =
    "items.locations.license=cc-by&genres.label=ThisIsNotAGenre&aggregations=items.locations.license"
  val redundantFilterBucket: String = """{
                                       |            "id" : "online",
                                       |            "label" : "Online",
                                       |            "type" : "Availability"
                                       |          }""".stripMargin
  val bogusValueParams: String =
    "items.locations.license=to-kill&genres.label=ThisIsNotAGenre&aggregations=items.locations.license"
}
