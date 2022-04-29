package weco.api.search.works

class WorksTestInvisible extends ApiWorksTestBase {
  val invisibleWorkIds =
    Seq("works.invisible.0", "works.invisible.1", "works.invisible.2")

  it("returns an HTTP 410 Gone if looking up a work with visible = false") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(worksIndex, invisibleWorkIds: _*)

        assertJsonResponse(routes, path = s"$rootPath/works/fixxlrcz") {
          Status.Gone -> deleted
        }
    }
  }

  it("excludes works with visible=false from list results") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(worksIndex, invisibleWorkIds: _*)

        assertJsonResponse(routes, path = s"$rootPath/works") {
          Status.OK -> emptyJsonResult
        }
    }
  }

  it("excludes works with visible=false from search results") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(
          worksIndex,
          "work.invisible.title-mouse",
          "work-title-mouse"
        )

        assertJsonResponse(routes, s"$rootPath/works?query=mouse") {
          Status.OK ->
            """
              |{
              |  "type": "ResultList",
              |  "pageSize": 10,
              |  "totalPages": 1,
              |  "totalResults": 1,
              |  "results": [
              |    {
              |      "id" : "b2tsq547",
              |      "title" : "A mezzotint of a mouse",
              |      "alternativeTitles" : [],
              |      "availabilities" : [],
              |      "type" : "Work"
              |    }
              |  ]
              |}
              |""".stripMargin
        }
    }
  }
}
