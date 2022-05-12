package weco.api.items.json

import io.circe.parser._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class JsonOpsTest extends AnyFunSpec with Matchers with JsonOps {
  describe("identifierType") {
    it("returns None if the object has no identifiers") {
      val json = parse("""
          |{ "color": "red", "sides": 5 }
          |""".stripMargin).right.get

      json.identifierType shouldBe None
    }

    it("returns None if there are no identifiers") {
      val json = parse("""
          |{ "color": "blue", "identifiers": [] }
          |""".stripMargin).right.get

      json.identifierType shouldBe None
    }

    it("returns the first identifier type") {
      val json = parse("""
          |{
          |  "color": "blue",
          |  "identifiers": [
          |    {
          |      "identifierType": {"id": "sierra-system-number"},
          |      "value": "b12345678"
          |    },
          |    {
          |      "identifierType": {"id": "calm-refno"},
          |      "value": "PP/CRI/1/2"
          |    }
          |  ]
          |}
          |""".stripMargin).right.get

      json.identifierType shouldBe Some("sierra-system-number")
    }
  }
}
