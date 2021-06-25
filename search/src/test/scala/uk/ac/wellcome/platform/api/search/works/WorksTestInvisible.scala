package uk.ac.wellcome.platform.api.search.works

import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed

class WorksTestInvisible extends ApiWorksTestBase {
  val invisibleWork: Work.Invisible[Indexed] =
    indexedWork().title("This work is invisible").invisible()

  it("returns an HTTP 410 Gone if looking up a work with visible = false") {
    withWorksApi {
      case (worksIndex, routes) =>
        insertIntoElasticsearch(worksIndex, invisibleWork)
        val path = s"$rootPath/works/${invisibleWork.state.canonicalId}"
        assertJsonResponse(routes, path) {
          Status.Gone -> deleted
        }
    }
  }

  it("excludes works with visible=false from list results") {
    withWorksApi {
      case (worksIndex, routes) =>
        val works = indexedWorks(count = 2).sortBy {
          _.state.canonicalId
        }

        val worksToIndex = Seq[Work[Indexed]](invisibleWork) ++ works
        insertIntoElasticsearch(worksIndex, worksToIndex: _*)

        assertJsonResponse(routes, s"$rootPath/works") {
          Status.OK -> worksListResponse(works = works)
        }
    }
  }

  it("excludes works with visible=false from search results") {
    withWorksApi {
      case (worksIndex, routes) =>
        val work = indexedWork().title("This shouldn't be invisible!")
        insertIntoElasticsearch(worksIndex, work, invisibleWork)

        assertJsonResponse(routes, s"$rootPath/works?query=invisible") {
          Status.OK -> worksListResponse(works = Seq(work))
        }
    }
  }
}
