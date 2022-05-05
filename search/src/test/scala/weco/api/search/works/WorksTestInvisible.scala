package weco.api.search.works

import weco.api.search.fixtures.TestDocumentFixtures
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed

class WorksTestInvisible extends ApiWorksTestBase with TestDocumentFixtures {
  it("returns an HTTP 410 Gone if looking up a work with visible = false") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "works.invisible.0")

        assertJsonResponse(routes, path = s"$rootPath/works/ocx5hlvi") {
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
