package weco.api.search.works

import org.scalatest.funspec.AnyFunSpec

class WorksTestDeleted extends AnyFunSpec with ApiWorksTestBase {
  it("returns an HTTP 410 Gone if looking up a deleted work") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "works.deleted.0")

        assertJsonResponse(routes, path = s"$rootPath/works/pnwuzuig") {
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
