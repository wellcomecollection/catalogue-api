package weco.api.search.models.index

import io.circe.Json

// Fetched by id only: the similar-images knn query needs the 4096-float vector.
case class IndexedImage(display: Json, vectorValues: Json) {
  lazy val features: Seq[Float] =
    vectorValues.hcursor
      .downField("features")
      .as[Seq[Float]]
      .right
      .get
}

object IndexedImage {
  // Search hit without vectorValues: hits never need the feature vector and it is large.
  case class Display(display: Json)
}
