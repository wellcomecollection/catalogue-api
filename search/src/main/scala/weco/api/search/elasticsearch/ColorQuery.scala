package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.requests.searches.knn.Knn
import weco.api.search.models.RgbColor


class ColorQuery {
  def apply(
     color: RgbColor,
  ): Knn =
    Knn(
      field = "query.inferredData.paletteEmbedding",
      numCandidates = 10000,
      queryVector = getColorSignature(color),
      k = 1,
      boost = 15,
//      similarity = Some(0.01.toFloat)
    )

  // This replicates the logic in palette_encoder.py:get_bin_index
  private def getColorSignature(color: RgbColor): Seq[Double] = {
    // palette_inferrer creates embeddings with 6 bins per colour dimension
    val n_bins = 6

    val r_index = (color.r / 255) * (n_bins - 1)
    val g_index = (color.g / 255) * (n_bins - 1)
    val b_index = (color.b / 255) * (n_bins - 1)

    val embedding_index = (b_index * n_bins * n_bins) + (r_index * n_bins) + g_index
    val embedding = Seq.fill(n_bins * n_bins * n_bins)(0.0).updated(embedding_index, 1.0)
//    println(embedding)
    embedding
  }
}
// rgb
// rbg
// gbr
// grb
// bgr
// brg
