package weco.api.search.models.index

import io.circe.Json

case class IndexedImage(display: Json, vectorValues: Json) {
  lazy val features: Seq[Float] =
    vectorValues.hcursor
      .downField("features")
      .as[Seq[Float]]
      .right
      .get
}
