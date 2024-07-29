package weco.api.search.models

import org.apache.pekko.http.scaladsl.model.Uri
import org.scalatest.Inside
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ApiConfigTest extends AnyFunSpec with Matchers with Inside {
  it("correctly parses a full root URI into its constituent parts") {
    val publicRoot = "https://api.wellcomecollection.org/catalogue/v2"
    inside(
      ApiConfig(
        publicRootUri = Uri(publicRoot),
        defaultPageSize = 10
      )
    ) {
      case apiConfig@ApiConfig(publicScheme, publicHost, publicRootPath, _) =>
        publicScheme shouldBe "https"
        publicHost shouldBe "api.wellcomecollection.org"
        publicRootPath shouldBe "/catalogue/v2"
        apiConfig.environment shouldBe ApiEnvironment.Prod
    }
  }

  it("correctly identifies a stage environment") {
    val publicRoot = "https://api-stage.wellcomecollection.org/catalogue/v2"
    inside(
      ApiConfig(
        publicRootUri = Uri(publicRoot),
        defaultPageSize = 10
      )
    ) {
      case apiConfig@ApiConfig(publicScheme, publicHost, publicRootPath, _) =>
        publicScheme shouldBe "https"
        publicHost shouldBe "api-stage.wellcomecollection.org"
        publicRootPath shouldBe "/catalogue/v2"
        apiConfig.environment shouldBe ApiEnvironment.Stage
    }
  }

  it("correctly identifies a dev environment") {
    val publicRoot = "https://localhost:8080/catalogue/v2"
    inside(
      ApiConfig(
        publicRootUri = Uri(publicRoot),
        defaultPageSize = 10
      )
    ) {
      case apiConfig@ApiConfig(publicScheme, publicHost, publicRootPath, _) =>
        publicScheme shouldBe "https"
        publicHost shouldBe "localhost"
        publicRootPath shouldBe "/catalogue/v2"
        apiConfig.environment shouldBe ApiEnvironment.Dev
    }
  }
}
