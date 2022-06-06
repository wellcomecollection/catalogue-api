package weco.api.search.works

class WorksRedirectsTest extends ApiWorksTestBase {
  val redirectSource = "4nwkprdt"
  val redirectTarget = "mv6kix0n"

  it("returns a TemporaryRedirect if looking up a redirected work") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "works.redirected.0")

        assertRedirectResponse(routes, path = s"$rootPath/works/$redirectSource") {
          Status.Found -> s"${apiConfig.publicRootPath}/works/$redirectTarget"
        }
    }
  }

  it("preserves query parameters on a 302 Redirect") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "works.redirected.0")

        assertRedirectResponse(
          routes,
          path = s"$rootPath/works/$redirectSource?include=identifiers"
        ) {
          Status.Found -> s"${apiConfig.publicRootPath}/works/$redirectTarget?include=identifiers"
        }
    }
  }
}
