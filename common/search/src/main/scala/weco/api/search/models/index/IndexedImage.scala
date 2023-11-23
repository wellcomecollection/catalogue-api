package weco.api.search.models.index

import io.circe.Json

case class IndexedImage(display: Json, vectorValues: Json) {
  lazy val reducedFeatures: Seq[Float] =
    vectorValues.hcursor
      .downField("reducedFeatures")
      .as[Seq[Float]]
      .right
      .get
}
