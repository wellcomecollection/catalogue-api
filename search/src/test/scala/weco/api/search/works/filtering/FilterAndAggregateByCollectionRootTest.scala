package weco.api.search.works.filtering

class FilterAndAggregateByCollectionRootTest
    extends SingleFieldFilterTest("collection.root")
    with FilteringTestCases
    with AggregatingTestCases {

  // Each work belongs to exactly one collection, and a root belongs to its own
  // collection. Across the corpus, gmf8ycys has three works, uy0hyncn has two
  // and pufll8p5 has one.
  val testWorks: Seq[String] = Seq(
    "works.archive.GC253.root",
    "works.archive.GC253.series",
    "works.archive.GC253.item",
    "works.archive.PPEBC.root",
    "works.archive.PPEBC.section",
    "works.archive.OH1.root"
  )

  val listingParams: String = "collection.root=gmf8ycys"
  val listingResponse: String = worksListResponse(
    ids = Seq(
      "works.archive.GC253.root",
      "works.archive.GC253.series",
      "works.archive.GC253.item"
    )
  )

  val multipleParams: String = "collection.root=uy0hyncn,pufll8p5"
  val multipleResponse: String = worksListResponse(
    ids = Seq(
      "works.archive.PPEBC.root",
      "works.archive.PPEBC.section",
      "works.archive.OH1.root"
    )
  )

  // "Correspondence" matches works in more than one collection: the title of
  // the GC253 series, and both PPEBC works (the title of the section, the
  // description of the root). The collection filter narrows it to the latter two.
  val searchingParams: String = "query=Correspondence&collection.root=uy0hyncn"
  val searchingResponse: String = worksListResponse(
    ids = Seq(
      "works.archive.PPEBC.root",
      "works.archive.PPEBC.section"
    )
  )

  val allValuesParams: String =
    "collection.root=pufll8p5&aggregations=collection.root"
  val allValuesResponse: String = worksListResponseWithAggs(
    Seq("works.archive.OH1.root"),
    Map(
      "collection.root" -> Seq(
        (3, "gmf8ycys", "Papers relating to the history of vaccination"),
        (2, "uy0hyncn", "Papers of Ernst Boris Chain"),
        (1, "pufll8p5", "Oral histories of British science")
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
    "collection.root=gmf8ycys&genres.label=NotAGenre&aggregations=collection.root"
  val redundantFilterBucket: String =
    """
      |{
      |            "id" : "gmf8ycys",
      |            "label" : "Papers relating to the history of vaccination"
      |          }
      |""".stripMargin

  val aggregationName: String = "collection.root"
  val unattestedValueParams: String =
    "collection.root=xyz&genres.label=NotAGenre&aggregations=collection.root"
  val bogusValueResponse: String = ""
}
