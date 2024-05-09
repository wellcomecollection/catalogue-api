package weco.api.search.models

import scala.util.Try
case class RgbColor(r: Float, g: Float, b: Float)

object RgbColor {
  def fromHex(color: String): Try[RgbColor] = Try {
    val n = Integer.parseInt(color, 16)
    val (r, g, b) = (
      (n >> 16) & 0xff,
      (n >> 8) & 0xff,
      n & 0xff
    )
    RgbColor(r, g, b)
  }
}
