package weco.api.search.works.filtering

class FilterAndAggregateByFormatTest
    extends FilteringTestCases
    with AggregatingTestCases {

  val testWorks = worksFormatBooks ++ worksFormatJournals ++ worksFormatAudio ++ worksFormatPictures

  val listingParams: String = "workType=k"
  val listingResponse: String = worksListResponse(
    ids = Seq("works.formats.9.Pictures")
  )
  val multipleParams: String = "workType=k,d"
  val multipleResponse: String = worksListResponse(
    ids = worksFormatJournals ++ worksFormatPictures
  )
  val searchingParams: String = "query=Book&workType=k"
  val searchingResponse: String = worksListResponse(ids = Nil)

  val aggregationName: String = "workType"
  val allValuesParams: String = "workType=k&aggregations=workType"
  val allValuesResponse: String = worksListResponseWithAggs(
    worksFormatPictures,
    Map(
      "workType" -> Seq(
        (4, "a", "Books"),
        (3, "d", "Journals"),
        (2, "i", "Audio"),
        (1, "k", "Pictures")
      ).map {
        case (count, identifier, label) =>
          (count, s"""
               |{
               |            "id" : "$identifier",
               |            "label" : "$label",
               |            "type" : "Format"
               |          }
               |""".stripMargin)
      }
    )
  )
  val redundantFilterParams: String =
    "workType=k&genres.label=ThisIsNotAGenre&aggregations=workType"
  val redundantFilterBucket: String = """{
                                       |            "id" : "k",
                                       |            "label" : "Pictures",
                                       |            "type" : "Format"
                                       |          }""".stripMargin
  val bogusValueParams: String =
    "workType=z&genres.label=ThisIsNotAGenre&aggregations=workType"
}
