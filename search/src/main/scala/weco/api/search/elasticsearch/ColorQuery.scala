package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.requests.searches.knn.Knn
import weco.api.search.models.RgbColor

class ColorQuery {
  // palette_inferrer creates embeddings with 6 bins per colour dimension
  private val n_bins = 6
  def apply(
    color: Option[RgbColor]
  ): Knn =
    color match {
      case Some(color) =>
        Knn(
          field = "query.inferredData.paletteEmbedding",
          numCandidates = 10000,
          queryVector = getColorSignature(color),
          k = 1000,
          boost = 15
        )
      // the idea here was to return a "dummy" Knn with a "neutral" queryVector if there's no color filter
      // unfortunately I can't make one that is valid and wouldn't affect the results
      case None =>
        Knn(
          field = "query.inferredData.paletteEmbedding",
          numCandidates = 10000,
          queryVector = Seq.fill(n_bins * n_bins * n_bins)(1.0 / 216),
          k = 1000,
          boost = 0
        )
    }

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
