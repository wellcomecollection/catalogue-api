package weco.api.search.elasticsearch

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.models.RgbColor
class ColorQueryTest extends AnyFunSpec with Matchers {
  val yellow: RgbColor = RgbColor.fromHex("ffff00").get

  it("creates knn search for signature with given hex color") {
    val query = ColorQuery(yellow)

    query.field shouldBe "query.inferredData.paletteEmbedding"
    query.queryVector should have length (1000)
  }
}
