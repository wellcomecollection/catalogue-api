package weco.api.search.works.filtering

class FilterByType extends FilteringTestCases {
  val testWorks: Seq[String] = Seq(
    "works.examples.different-work-types.Collection",
    "works.examples.different-work-types.Series",
    "works.examples.different-work-types.Section"
  )
  val listingParams: String = "type=Collection"
  val listingResponse: String = worksListResponse(
    ids = Seq("works.examples.different-work-types.Collection")
  )
  val multipleParams: String = "type=Collection,Series"
  val multipleResponse: String = worksListResponse(
    ids = Seq(
      "works.examples.different-work-types.Collection",
      "works.examples.different-work-types.Series"
    )
  )
  val searchingParams: String = "query=rats&type=Series,Section"
  val searchingResponse: String = worksListResponse(
    ids = Seq(
      "works.examples.different-work-types.Series",
      "works.examples.different-work-types.Section"
    )
  )
}
