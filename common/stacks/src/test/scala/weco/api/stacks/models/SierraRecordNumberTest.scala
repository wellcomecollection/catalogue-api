package weco.api.stacks.models

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class SierraRecordNumberTest extends AnyFunSpec with Matchers {
  describe("withCheckDigit") {
    it("handles an item example from the catalogue") {
      SierraItemNumber("1828888").withCheckDigit shouldBe "i18288881"
    }

    it("adds an 'x' where necessary") {
      SierraItemNumber("1840974").withCheckDigit shouldBe "i1840974x"
    }
  }

  describe("withoutCheckDigit") {
    it("handles an item example") {
      SierraItemNumber("1828888").withoutCheckDigit shouldBe "1828888"
    }
  }

  it("can create a Sierra item number from an ID with a check digit") {
    val s = SierraItemNumber("i12345678")
    s.withoutCheckDigit shouldBe "1234567"
  }

  it("throws an error if passed a Sierra ID which is non-numeric") {
    assertStringFailsValidation("abcdefg")
  }

  it("throws an error if passed a Sierra ID which is too short") {
    assertStringFailsValidation("123")
  }

  it("throws an error if passed a Sierra ID which is too long") {
    assertStringFailsValidation("12345678")
  }

  private def assertStringFailsValidation(recordNumber: String) = {
    val caught = intercept[IllegalArgumentException] {
      SierraItemNumber(recordNumber)
    }

    caught.getMessage shouldEqual s"requirement failed: Not a 7-digit Sierra record number: $recordNumber"
  }
}
