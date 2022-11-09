package weco.api.search.models.index

import io.circe.Json

case class IndexedImage(display: Json, query: Json) {
  lazy val reducedFeatures: Seq[Float] =
    query.hcursor
      .downField("inferredData")
      .downField("reducedFeatures")
      .as[Seq[Float]]
      .right
      .get
}
