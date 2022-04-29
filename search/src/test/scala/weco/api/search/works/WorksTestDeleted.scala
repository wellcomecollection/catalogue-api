package weco.api.search.works

class WorksTestDeleted extends ApiWorksTestBase {
  it("returns an HTTP 410 Gone if looking up a deleted work") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "works.deleted.0")

        assertJsonResponse(routes, path = s"$rootPath/works/batmoife") {
          Status.Gone -> deleted
        }
    }
  }

  it("excludes deleted works") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, visibleWorks: _*)
        indexTestDocuments(worksIndex, deletedWorks: _*)

        assertJsonResponse(routes, s"$rootPath/works") {
          Status.OK -> newWorksListResponse(visibleWorks)
        }
    }
  }
}
