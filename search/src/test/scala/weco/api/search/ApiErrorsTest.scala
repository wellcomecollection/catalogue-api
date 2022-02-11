package weco.api.search

import weco.api.search.works.ApiWorksTestBase

class ApiErrorsTest extends ApiWorksTestBase {
  it("returns a Not Found error if you try to get an unrecognised path") {
    withApi { route =>
      assertNotFound(route)(
        path = s"$rootPath/foo/bar",
        description =
          s"Page not found for URL ${apiConfig.publicRootPath}/foo/bar"
      )
    }
  }
}
