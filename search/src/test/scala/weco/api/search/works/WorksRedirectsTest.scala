package weco.api.search.works

import weco.api.search.fixtures.TestDocumentFixtures

class WorksRedirectsTest extends ApiWorksTestBase with TestDocumentFixtures {
  it("returns a TemporaryRedirect if looking up a redirected work") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestWorks(worksIndex, "works.redirected.0")

        assertRedirectResponse(routes, path = s"$rootPath/works/yemd0v8n") {
          Status.Found -> s"${apiConfig.publicRootPath}/works/qklbwm3u"
        }
    }
  }

  it("preserves query parameters on a 302 Redirect") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestWorks(worksIndex, "works.redirected.0")

        assertRedirectResponse(
          routes,
          path = s"$rootPath/works/yemd0v8n?include=identifiers"
        ) {
          Status.Found -> s"${apiConfig.publicRootPath}/works/qklbwm3u?include=identifiers"
        }
    }
  }
}
