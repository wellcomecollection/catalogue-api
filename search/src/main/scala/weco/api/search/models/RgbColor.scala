package weco.api.search.models

import scala.util.Try
//import java.awt.Color

case class RgbColor(r: Float, g: Float, b: Float)

object RgbColor {
  def fromHex(color: String): Try[RgbColor] = Try {
    val n = Integer.parseInt(color, 16)
    val (r, g, b) = (
      (n >> 16) & 0xFF,
      (n >> 8) & 0xFF,
      n & 0xFF
    )
    RgbColor(r, g, b)
  }
}
