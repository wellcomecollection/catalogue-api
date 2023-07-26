package weco.api.search.elasticsearch

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher}
import weco.api.search.models.RgbColor
//import io.circe.Json

//case class ColorQuery(field: String, query_vector: List[Int], k: Int, num_candidates: Int, boost: Int)

trait CustomMatchers {
  def field(expectedValue: String): HavePropertyMatcher[ColorQuery, String] =
    (colorQuery: ColorQuery) => HavePropertyMatchResult(
      colorQuery.field == expectedValue,
      "field",
      expectedValue,
      colorQuery.field
    )

  def queryVector(expectedValue: List[Int]): HavePropertyMatcher[ColorQuery, List[Int]] =
    (colorQuery: ColorQuery) => HavePropertyMatchResult(
      colorQuery.query_vector == expectedValue,
      "query_vector",
      expectedValue,
      colorQuery.query_vector
    )
}

class ColorQueryTest extends AnyFunSpec with Matchers with CustomMatchers {
//  val colorQuery = new ColorQuery()

//  val red: RgbColor= RgbColor.fromHex("ff0000").get
//  val green: RgbColor = RgbColor.fromHex("00ff00").get
//  val blue: RgbColor = RgbColor.fromHex("0000ff").get
  val yellow: RgbColor = RgbColor.fromHex("ffff00").get

  it("creates query for signature with given hex color") {
    val query = ColorQuery.apply(color = yellow)
    println(query)
//    query should have (
//      field ("query.inferredData.paletteEmbedding"),
//      queryVector (List(0, 0, 0, 0, 1))
//    )
  }
}
