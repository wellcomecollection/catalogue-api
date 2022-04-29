package weco.api.search.works

class WorksRedirectsTest extends ApiWorksTestBase {
  it("returns a TemporaryRedirect if looking up a redirected work") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(worksIndex, "works.redirected.0")

        assertRedirectResponse(routes, path = s"$rootPath/works/iyop2wz6") {
          Status.Found -> s"${apiConfig.publicRootPath}/works/d0v8n5yz"
        }
    }
  }

  it("preserves query parameters on a 302 Redirect") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(worksIndex, "works.redirected.0")

        assertRedirectResponse(
          routes,
          path = s"$rootPath/works/iyop2wz6?include=identifiers"
        ) {
          Status.Found -> s"${apiConfig.publicRootPath}/works/d0v8n5yz?include=identifiers"
        }
    }
  }
}
