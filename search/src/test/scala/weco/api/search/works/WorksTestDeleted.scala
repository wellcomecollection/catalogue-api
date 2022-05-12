package weco.api.search.works

import weco.api.search.fixtures.TestDocumentFixtures

class WorksTestDeleted extends ApiWorksTestBase with TestDocumentFixtures {
  it("returns an HTTP 410 Gone if looking up a deleted work") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "works.deleted.0")

        assertJsonResponse(routes, path = s"$rootPath/works/batmoife") {
          Status.Gone -> deleted
        }
    }
  }

  it("excludes deleted works from results") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, deletedWorks: _*)

        assertJsonResponse(routes, path = s"$rootPath/works") {
          Status.OK -> emptyJsonResult
        }
    }
  }
}
