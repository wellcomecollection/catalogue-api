package weco.api.search.works

class WorksTestInvisible extends ApiWorksTestBase {
  it("returns an HTTP 410 Gone if looking up a work with visible = false") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "works.invisible.0")

        assertJsonResponse(routes, path = s"$rootPath/works/rczekocx") {
          Status.Gone -> deleted
        }
    }
  }

  it("excludes invisible works from results") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, invisibleWorks: _*)

        assertJsonResponse(routes, path = s"$rootPath/works") {
          Status.OK -> emptyJsonResult
        }
    }
  }
}
