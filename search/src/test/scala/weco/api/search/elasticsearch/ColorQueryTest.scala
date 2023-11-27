package weco.api.search.elasticsearch

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.models.RgbColor
class ColorQueryTest extends AnyFunSpec with Matchers {
  val yellow: RgbColor = RgbColor.fromHex("ffff00").get

  it("creates knn search for signature with given hex color") {
    val query = ColorQuery(yellow)

    query.queryVector should have length 1000
    val norm = query.queryVector.map(x => x * x).sum
    approxEquals(norm, 1.0) shouldBe true
  }

  // Got to deal with floating point precision
  def approxEquals(a: Double, b: Double): Boolean = {
    val epsilon = 1e-7
    (a - b).abs < epsilon
  }
}
