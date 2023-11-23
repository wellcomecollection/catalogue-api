package weco.api.search.elasticsearch

import weco.api.search.models.index.IndexedImage
import io.circe.syntax._
import io.circe.{Json, JsonObject}

case object ImageSimilarity {
  def features: (String, IndexedImage) => JsonObject =
    knnQuery("vectorValues.reducedFeatures")

  private def knnQuery(
    field: String
  )(imageId: String, image: IndexedImage): JsonObject =
    Json
      .obj(
        "knn" -> Json.obj(
          "field" -> field.asJson,
          "query_vector" -> image.reducedFeatures.asJson,
          "k" -> 10.asJson,
          "num_candidates" -> 100.asJson,
          "filter" -> Json.obj(
            "bool" -> Json.obj(
              "must_not" -> Json.obj(
                "ids" -> Json.obj(
                  "values" -> Json.arr(Json.fromString(imageId))
                )
              )
            )
          )
        )
      )
      .asObject
      .get
}
