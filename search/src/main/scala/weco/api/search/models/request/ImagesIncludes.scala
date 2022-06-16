package weco.api.search.models.request

sealed trait ImageInclude

object ImageInclude {
  case object VisuallySimilar extends ImageInclude
  case object WithSimilarFeatures extends ImageInclude
  case object WithSimilarColors extends ImageInclude
  case object SourceContributors extends ImageInclude
  case object SourceLanguages extends ImageInclude
  case object SourceGenres extends ImageInclude
  case object SourceSubjects extends ImageInclude
}

sealed trait ImageIncludes {
  val visuallySimilar: Boolean
  val withSimilarFeatures: Boolean
  val withSimilarColors: Boolean
  val `source.contributors`: Boolean
  val `source.languages`: Boolean
  val `source.genres`: Boolean
  val `source.subjects`: Boolean
}

case class SingleImageIncludes(
  visuallySimilar: Boolean,
  withSimilarFeatures: Boolean,
  withSimilarColors: Boolean,
  `source.contributors`: Boolean,
  `source.languages`: Boolean,
  `source.genres`: Boolean,
  `source.subjects`: Boolean
) extends ImageIncludes

object SingleImageIncludes {
  import ImageInclude._

  def apply(includes: ImageInclude*): SingleImageIncludes =
    SingleImageIncludes(
      visuallySimilar = includes.contains(VisuallySimilar),
      withSimilarFeatures = includes.contains(WithSimilarFeatures),
      withSimilarColors = includes.contains(WithSimilarColors),
      `source.contributors` = includes.contains(SourceContributors),
      `source.languages` = includes.contains(SourceLanguages),
      `source.genres` = includes.contains(SourceGenres),
      `source.subjects` = includes.contains(SourceSubjects)
    )

  def none: SingleImageIncludes = SingleImageIncludes()
}

case class MultipleImagesIncludes(
  `source.contributors`: Boolean,
  `source.languages`: Boolean,
  `source.genres`: Boolean,
  `source.subjects`: Boolean
) extends ImageIncludes {
  val visuallySimilar: Boolean = false
  val withSimilarFeatures: Boolean = false
  val withSimilarColors: Boolean = false
}

object MultipleImagesIncludes {
  import ImageInclude._

  def apply(includes: ImageInclude*): MultipleImagesIncludes =
    MultipleImagesIncludes(
      `source.contributors` = includes.contains(SourceContributors),
      `source.languages` = includes.contains(SourceLanguages),
      `source.genres` = includes.contains(SourceGenres),
      `source.subjects` = includes.contains(SourceSubjects)
    )

  def none: MultipleImagesIncludes = MultipleImagesIncludes()
}
