package uk.ac.wellcome.platform.api.search.works

import uk.ac.wellcome.models.Implicits._
import weco.catalogue.internal_model.identifiers.IdState

class WorksRedirectsTest extends ApiWorksTestBase {

  val redirectedWork = indexedWork().redirected(
    IdState.Identified(
      canonicalId = createCanonicalId,
      sourceIdentifier = createSourceIdentifier
    )
  )
  val redirectId = redirectedWork.redirectTarget.canonicalId

  it("returns a TemporaryRedirect if looking up a redirected work") {
    withWorksApi {
      case (worksIndex, routes) =>
        insertIntoElasticsearch(worksIndex, redirectedWork)
        val path = s"$rootPath/works/${redirectedWork.state.canonicalId}"
        assertRedirectResponse(routes, path) {
          Status.Found -> s"$rootPath/works/$redirectId"
        }
    }
  }

  it("preserves query parameters on a 302 Redirect") {
    withWorksApi {
      case (worksIndex, routes) =>
        insertIntoElasticsearch(worksIndex, redirectedWork)
        val path =
          s"$rootPath/works/${redirectedWork.state.canonicalId}?include=identifiers"
        assertRedirectResponse(routes, path) {
          Status.Found -> s"$rootPath/works/$redirectId?include=identifiers"
        }
    }
  }
}
