package weco.api.search.json

import io.circe.Json
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import io.circe.parser._
import org.scalatest.EitherValues

class JsonOpsTest extends AnyFunSpec with Matchers with EitherValues {
  import JsonOps._

  describe("removeKey") {
    it("removes a key if the JSON is an object and the key is present") {
      val json = parseObject("""
          |{
          |  "name": "square",
          |  "colour": "red"
          |}
          |""")

      val expectedJson = parseObject("""
          |{
          |  "name": "square"
          |}
          |""")

      json.removeKey("colour") shouldBe expectedJson
    }

    it(
      "leaves the JSON as-is if the JSON is an object and the key is not present"
    ) {
      val json = parseObject("""
          |{
          |  "name": "square",
          |  "colour": "red"
          |}
          |""")

      json.removeKey("sides") shouldBe json
    }

    it("leaves the JSON as-is if the JSON is not an object") {
      val json = parseJson("""
          |"blue triangle"
          |""")

      json.removeKey("sides") shouldBe json
    }

    it("does not remove keys nested below the top level") {
      val json = parseObject("""
          |{
          |  "name": "hexagon",
          |  "colour": {
          |    "hue": 22,
          |    "saturation": 100,
          |    "lightness": 1
          |  }
          |}
          |""")

      json.removeKey("saturation") shouldBe json
    }
  }

  describe("removeKeyRecursively") {
    it("removes a key if the JSON is an object and the key is present") {
      val json = parseObject("""
          |{
          |  "name": "square",
          |  "colour": "red"
          |}
          |""")

      val expectedJson = parseObject("""
          |{
          |  "name": "square"
          |}
          |""")

      json.removeKeyRecursively("colour") shouldBe expectedJson
    }

    it(
      "leaves the JSON as-is if the JSON is an object and the key is not present"
    ) {
      val json = parseObject("""
          |{
          |  "name": "square",
          |  "colour": "red"
          |}
          |""")

      json.removeKeyRecursively("sides") shouldBe json
    }

    it("leaves the JSON as-is if the JSON is not an object") {
      val json = parseJson("""
          |"blue triangle"
          |""")

      json.removeKeyRecursively("sides") shouldBe json
    }

    it("removes keys nested below the top level") {
      val json = parseObject("""
          |{
          |  "name": "hexagon",
          |  "colour": {
          |    "hue": 22,
          |    "saturation": 100,
          |    "lightness": 1
          |  },
          |  "side_lengths": [20, 20, 20, 30, 40, 50]
          |}
          |""")

      val expectedJson = parseObject("""
          {
          |  "name": "hexagon",
          |  "colour": {
          |    "hue": 22,
          |    "lightness": 1
          |  },
          |  "side_lengths": [20, 20, 20, 30, 40, 50]
          |}
          |""")

      json.removeKeyRecursively("saturation") shouldBe expectedJson
    }

    it("removes keys nested in arrays") {
      val json = parseObject(
        """
          |{
          |  "name": "hexagon",
          |  "colours": [
          |    { "hue": 22, "saturation": 100, "lightness": 1 },
          |    { "hue": 92, "saturation": 100, "lightness": 1 },
          |    { "hue": 162, "saturation": 100, "lightness": 1 }
          |  ]
          |}"""
      )

      val expectedJson = parseObject(
        """
          |{
          |  "name": "hexagon",
          |  "colours": [
          |    { "hue": 22, "lightness": 1 },
          |    { "hue": 92, "lightness": 1 },
          |    { "hue": 162, "lightness": 1 }
          |  ]
          |}"""
      )

      json.removeKeyRecursively("saturation") shouldBe expectedJson
    }
  }

  def parseJson(s: String): Json =
    parse(s.stripMargin).right.value

  def parseObject(s: String): Json = {
    val j = parseJson(s)
    j.isObject shouldBe true
    j
  }
}
