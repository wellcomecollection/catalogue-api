package weco.api.search.elasticsearch

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.models.HsvColor

class ColorQueryTest extends AnyFunSpec with Matchers {
  val colorQuery = new ColorQuery(
    binSizes = Seq(Seq(4, 6, 9), Seq(2, 4, 6), Seq(1, 3, 5)),
    binMinima = Seq(0f, 10f / 256, 10f / 256)
  )

  val red: HsvColor = HsvColor.fromHex("ff0000").get
  val green: HsvColor = HsvColor.fromHex("00ff00").get
  val blue: HsvColor = HsvColor.fromHex("0000ff").get
  val yellow: HsvColor = HsvColor.fromHex("ffff00").get

  it("creates queries for signatures with given bin sizes") {
    val q = colorQuery("colorField", colors = Seq(yellow))

    q.fields should contain only "colorField"
    q.likeTexts shouldBe Seq(
      "7/0",
      "71/1",
      "269/2"
    )
  }

  it("creates queries for signatures with only some bins used") {
    val q = colorQuery("colorField", colors = Seq(yellow), binIndices = Seq(2))

    q.fields should contain only "colorField"
    q.likeTexts shouldBe Seq("269/2")
  }

  it("creates queries for multiple colors") {
    val q = colorQuery("colorField", colors = Seq(red, green, blue))

    q.fields should contain only "colorField"
    q.likeTexts shouldBe Seq(
      "7/0",
      "8/0",
      "9/0",
      "71/1",
      "72/1",
      "74/1",
      "268/2",
      "270/2",
      "273/2"
    )
  }
}
