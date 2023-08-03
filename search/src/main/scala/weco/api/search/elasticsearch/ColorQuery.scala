package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.requests.searches.knn.Knn
import weco.api.search.models.RgbColor

object ColorQuery {
  // palette_inferrer creates embeddings with 6 bins per colour dimension
  private val n_bins = 6
  def apply(
    color: RgbColor
  ): Knn =
    Knn(
      field = "query.inferredData.paletteEmbedding",
      numCandidates = 10000,
      queryVector = getColorSignature(color),
      k = 1000,
      boost = 15
    )

  // This replicates the logic in palette_encoder.py:get_bin_index
  private def getColorSignature(color: RgbColor): Seq[Double] = {
    val r_index = (color.r / 255) * (n_bins - 1)
    val g_index = (color.g / 255) * (n_bins - 1)
    val b_index = (color.b / 255) * (n_bins - 1)

    val embedding_index = b_index + g_index * n_bins + r_index * n_bins * n_bins
    Seq
      .fill(n_bins * n_bins * n_bins)(0.0)
      .updated(embedding_index.toInt, 1.0)
  }
}
