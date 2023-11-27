package weco.api.search.elasticsearch

import weco.api.search.models.RgbColor
import weco.api.search.services.SearchTemplateKNNParams

import scala.math.pow

object ColorQuery {
  // palette_inferrer creates embeddings with 10 bins per colour dimension
  private val n_bins = 10
  def apply(
    color: RgbColor
  ): SearchTemplateKNNParams =
    SearchTemplateKNNParams(
      field = "vectorValues.paletteEmbedding",
      numCandidates = 10000,
      queryVector = getColorSignature(color),
      k = 1000
    )

  // This replicates the logic in palette_encoder.py:get_bin_index
  private def getColorSignature(color: RgbColor): Seq[Double] = {
    val r_index = Math.round((color.r / 255) * (n_bins - 1))
    val g_index = Math.round((color.g / 255) * (n_bins - 1))
    val b_index = Math.round((color.b / 255) * (n_bins - 1))

    val embedding_index = b_index + g_index * n_bins + r_index * n_bins * n_bins
    var embedding = Seq
      .fill(n_bins * n_bins * n_bins)(0.0)
      .updated(embedding_index, 1.0)

    // add a 3d blur around the target index to make the embedding robust to small
    // changes in colour
    val sigma = 0.3
    def gaussian(x: Double, y: Double, z: Double): Double =
      Math.exp(
        -(pow(x - r_index, 2) + pow(y - g_index, 2) + pow(z - b_index, 2)) /
          (2 * pow(sigma, 2))
      )

    for (x <- 0 until n_bins) {
      for (y <- 0 until n_bins) {
        for (z <- 0 until n_bins) {
          val i = z + y * n_bins + x * n_bins * n_bins
          embedding = embedding.updated(i, gaussian(x, y, z))
        }
      }
    }
    val norm = Math.sqrt(embedding.foldLeft(0.0)((a, b) => a + b * b))
    embedding.map((x) => x / norm)
  }
}
