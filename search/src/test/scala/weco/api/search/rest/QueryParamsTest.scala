package weco.api.search.rest

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import io.circe.Json
import weco.api.search.rest.QueryParamsUtils.IncludesAndExcludes

class QueryParamsTest extends AnyFunSpec with Matchers {

  describe("includes / excludes") {
    val decoder = QueryParamsUtils.decodeIncludesAndExcludes(validStrs = Set("a", "b"))

    def decode(str: String) =
      decoder.decodeJson(Json.fromString(str))

    it("decodes includes") {
      decode("a,b") shouldBe Right(IncludesAndExcludes(includes = List("a", "b"), excludes = Nil))
    }

    it("decodes excludes") {
      decode("!a,!b") shouldBe Right(IncludesAndExcludes(includes = Nil, excludes = List("a", "b")))
    }

    it("decodes a mixture of includes and excludes") {
      decode("a,!b") shouldBe Right(IncludesAndExcludes(includes = List("a"), excludes = List("b")))
    }

    it("fails decoding when unrecognised values") {
      val result = decode("b,c,!d")
      result shouldBe a[Left[_, _]]
      result.left.get.message shouldBe
        "'c', '!d' are not valid values. Please choose one of: ['a', 'b']"
    }
  }
}
