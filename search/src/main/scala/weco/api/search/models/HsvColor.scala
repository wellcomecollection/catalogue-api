package weco.api.search.models

import java.awt.{Color => AwtColor}
import scala.util.Try

case class HsvColor(hue: Float, saturation: Float, value: Float)

case object HsvColor {
  def fromHex(hexString: String): Try[HsvColor] = Try {
    val n = Integer.parseInt(hexString, 16)
    val (r, g, b) = (
      (n >> 16) & 0xFF,
      (n >> 8) & 0xFF,
      n & 0xFF
    )
    val hsv = AwtColor.RGBtoHSB(r, g, b, null)

    HsvColor(hue = hsv(0), saturation = hsv(1), value = hsv(2))
  }
}
