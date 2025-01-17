package weco.api.search.works.filtering

class FilterAndAggregateByLanguageTest
    extends SingleFieldFilterTest("language")
    with FilteringTestCases
    with AggregatingTestCases {
  val testWorks: Seq[String] = Seq(
    "works.languages.0.eng",
    "works.languages.1.eng",
    "works.languages.2.eng",
    "works.languages.3.eng+swe",
    "works.languages.4.eng+swe+tur",
    "works.languages.5.swe",
    "works.languages.6.tur"
  )
  val listingParams: String = "languages=eng"
  val listingResponse: String = worksListResponse(
    ids = Seq(
      "works.languages.0.eng",
      "works.languages.1.eng",
      "works.languages.2.eng",
      "works.languages.3.eng+swe",
      "works.languages.4.eng+swe+tur"
    )
  )

  val multipleParams: String = "languages=swe,tur"
  val multipleResponse: String = worksListResponse(
    ids = Seq(
      "works.languages.3.eng+swe",
      "works.languages.4.eng+swe+tur",
      "works.languages.5.swe",
      "works.languages.6.tur"
    )
  )

  val searchingParams: String = "query=Swedish&languages=tur"
  val searchingResponse: String = worksListResponse(
    ids = Seq(
      "works.languages.4.eng+swe+tur"
    )
  )

  val allValuesParams: String = "languages=tur&aggregations=languages"
  val allValuesResponse: String = worksListResponseWithAggs(
    Seq("works.languages.4.eng+swe+tur", "works.languages.6.tur"),
    Map(
      "languages" -> Seq(
        (5, "eng", "English"),
        (3, "swe", "Swedish"),
        (2, "tur", "Turkish")
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
    "languages=eng&genres.label=NotAGenre&aggregations=languages"
  val redundantFilterBucket: String =
    """
      |{
      |            "id" : "eng",
      |            "label" : "English"
      |          }
      |""".stripMargin
  val aggregationName: String = "languages"
  val unattestedValueParams: String =
    "languages=sjn&genres.label=NotAGenre&aggregations=languages"
  val bogusValueResponse: String = ""
}
