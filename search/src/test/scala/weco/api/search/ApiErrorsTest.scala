package weco.api.search

import org.scalatest.funspec.AnyFunSpec
import weco.api.search.works.ApiWorksTestBase

class ApiErrorsTest extends AnyFunSpec with ApiWorksTestBase {
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
