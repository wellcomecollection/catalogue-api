package weco.api.search.models

import org.scalatest.TryValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

class RgbColorTest
    extends AnyFunSpec
    with Matchers
    with TryValues
    with TableDrivenPropertyChecks {
  it("creates an RGB from a valid string") {
    val testCases = Table(
      ("hexString", "expectedColor"),
      ("ff0000", RgbColor(255, 0, 0)),
      ("00ff00", RgbColor(0,255,0)),
      ("0000ff", RgbColor(0,0, 255)),
      ("00fa9a", RgbColor(0,250,154))
    )

    forAll(testCases) {
      case (hexString, expectedColor) =>
        RgbColor.fromHex(hexString).success.value shouldBe expectedColor
    }
  }

  it("fails if passed something that isn't a hex string") {
    RgbColor.fromHex("XYZ").failure.exception shouldBe a[NumberFormatException]
  }
}
