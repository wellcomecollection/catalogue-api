package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.ElasticApi._
import com.sksamuel.elastic4s.requests.searches.queries.MoreLikeThisQuery
import weco.api.search.models.HsvColor

class ColorQuery(binSizes: Seq[Seq[Int]], binMinima: Seq[Float]) {
  require(
    binSizes.size == 3 && binSizes.forall(_.size == 3),
    "binSizes must be a 3x3 sequence of ints"
  )
  require(
    binMinima.size == 3,
    "binMinima must be a sequence of 3 floats"
  )

  private final val minBinWidth = 1f / 256

  lazy private val transposedSizes = binSizes.transpose
  lazy private val satMin = binMinima(1)
  lazy private val valMin = binMinima(2)

  def apply(
    field: String,
    colors: Seq[HsvColor],
    binIndices: Seq[Int] = binSizes.indices
  ): MoreLikeThisQuery =
    moreLikeThisQuery(field)
      .likeTexts(getColorsSignature(colors, binIndices))
      .copy(
        minTermFreq = Some(1),
        minDocFreq = Some(1),
        maxQueryTerms = Some(1000),
        minShouldMatch = Some("1")
      )
      .boost(2000)

  // This replicates the logic in palette_encoder.py:get_bin_index
  private def getColorsSignature(
    colors: Seq[HsvColor],
    binIndices: Seq[Int]
  ): Seq[String] =
    binIndices
      .map(transposedSizes)
      .zip(binIndices)
      .flatMap {
        case (nBins, i) =>
          val nValBins = nBins(1)
          colors
            .map {
              case c if c.value < valMin => 0
              case c if c.saturation < satMin =>
                1 + math
                  .floor(nValBins * (c.value - valMin) / (1 - valMin + minBinWidth))
                  .toInt
              case c =>
                def idx(x: Float, i: Int): Int = {
                  val num = nBins(i) * (x - binMinima(i))
                  val denom = 1 - binMinima(i) + minBinWidth
                  math.floor(num / denom).toInt
                }
                1 + nValBins +
                  idx(c.hue, 0) +
                  nBins(0) * idx(c.saturation, 1) +
                  nBins(0) * nBins(1) * idx(c.value, 2)
            }
            .map((_, i))
      }
      .map {
        case (binIndex, i) => s"$binIndex/$i"
      }

}
