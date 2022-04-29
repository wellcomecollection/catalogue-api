package weco.api.search.works

import weco.api.search.fixtures.TestDocumentFixtures

class WorksTestInvisible extends ApiWorksTestBase with TestDocumentFixtures {
  it("returns an HTTP 410 Gone if looking up an invisible work") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "works.invisible.0")

        assertJsonResponse(routes, path = s"$rootPath/works/ocx5hlvi") {
          Status.Gone -> deleted
        }
    }
  }

  it("excludes invisible works") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, visibleWorks: _*)
        indexTestDocuments(worksIndex, invisibleWorks: _*)

        assertJsonResponse(routes, s"$rootPath/works") {
          Status.OK -> newWorksListResponse(visibleWorks)
        }
    }
  }
}
