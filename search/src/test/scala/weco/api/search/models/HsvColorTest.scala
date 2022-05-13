package weco.api.search.models

import org.scalatest.TryValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

class HsvColorTest extends AnyFunSpec with Matchers with TryValues with TableDrivenPropertyChecks {
  it("creates an HSV from a valid string") {
    val testCases = Table(
      ("hexString", "expectedColor"),
      ("ff0000", HsvColor(0, 1, 1)),
      ("00ff00", HsvColor(120f / 360f, 1, 1)),
      ("0000ff", HsvColor(240f / 360f, 1, 1)),
      ("00fa9a", HsvColor(0.43600f, 1f, 0.98039216f)),
    )

    forAll(testCases) { case (hexString, expectedColor) =>
      HsvColor.fromHex(hexString).success.value shouldBe expectedColor
    }
  }

  it("fails if passed something that isn't a hex string") {
    HsvColor.fromHex("XYZ").failure.exception shouldBe a[NumberFormatException]
  }
}
