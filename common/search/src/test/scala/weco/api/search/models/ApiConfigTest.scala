package weco.api.search.models

import akka.http.scaladsl.model.Uri
import org.scalatest.Inside
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ApiConfigTest extends AnyFunSpec with Matchers with Inside {
  it("correctly parses a full root URI into its constituent parts") {
    val publicRoot = "https://api.wellcomecollection.org/catalogue/v2"
    inside(
      ApiConfig(
        publicRootUri = Uri(publicRoot),
        defaultPageSize = 10,
        contextSuffix = "context.json"
      )
    ) {
      case ApiConfig(publicScheme, publicHost, publicRootPath, _, _) =>
        publicScheme shouldBe "https"
        publicHost shouldBe "api.wellcomecollection.org"
        publicRootPath shouldBe "/catalogue/v2"
    }
  }
}
