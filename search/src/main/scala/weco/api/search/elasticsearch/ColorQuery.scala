package weco.api.search.elasticsearch

import io.circe.syntax._
import io.circe.{Json, JsonObject}
import weco.api.search.models.RgbColor

case class ColorQuery(field: String, query_vector: List[Int], k: Int, num_candidates: Int, boost: Int)

object ColorQuery {
  def apply(
     color: RgbColor,
  ): JsonObject =
    Json
      .obj(
        "field" -> "query.inferredData.paletteEmbedding".asJson,
        "query_vector" -> getColorsSignature(color).asJson,
        "k" -> 1000.asJson,
        "num_candidates" -> 10000.asJson,
        "boost" -> 15.asJson
      )
      .asObject
      .get


  // This replicates the logic in palette_encoder.py:get_bin_index
  private def getColorsSignature(color: RgbColor): List[Int] = {
    // palette_inferrer creates embeddings with 6 bins per colour dimension
    val n_bins = 6

    val r_index = (color.r / 255) * (n_bins - 1)
    val g_index = (color.g / 255) * (n_bins - 1)
    val b_index = (color.b / 255) * (n_bins - 1)

    val embedding_index = b_index + g_index * n_bins + r_index * n_bins * n_bins
    val embedding = List.fill(n_bins * n_bins * n_bins)(0)
    embedding.updated(embedding_index, 1)
  }
}
