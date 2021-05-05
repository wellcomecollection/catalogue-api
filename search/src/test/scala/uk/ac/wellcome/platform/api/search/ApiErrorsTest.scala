package uk.ac.wellcome.platform.api.search

import uk.ac.wellcome.platform.api.search.works.ApiWorksTestBase

class ApiErrorsTest extends ApiWorksTestBase {

  it("returns a Not Found error if you try to get a non-existent API version") {
    withApi { routes =>
      assertJsonResponse(routes, "/catalogue/v567/works") {
        Status.NotFound -> notFound(
          "Page not found for URL /catalogue/v567/works"
        )
      }
    }
  }

  it("returns a Not Found error if you try to get an unrecognised path") {
    withApi { routes =>
      assertJsonResponse(routes, s"$rootPath/foo/bar") {
        Status.NotFound -> notFound(
          s"Page not found for URL ${apiConfig.publicRootPath}/foo/bar"
        )
      }
    }
  }
}
