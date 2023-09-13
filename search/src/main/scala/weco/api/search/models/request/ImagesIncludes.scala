package weco.api.search.models.request

sealed trait ImageInclude

object ImageInclude {
  case object WithSimilarFeatures extends ImageInclude
  case object SourceContributors extends ImageInclude
  case object SourceLanguages extends ImageInclude
  case object SourceGenres extends ImageInclude
  case object SourceSubjects extends ImageInclude
}

sealed trait ImageIncludes {
  val withSimilarFeatures: Boolean
  val `source.contributors`: Boolean
  val `source.languages`: Boolean
  val `source.genres`: Boolean
  val `source.subjects`: Boolean
}

case class SingleImageIncludes(
  withSimilarFeatures: Boolean,
  `source.contributors`: Boolean,
  `source.languages`: Boolean,
  `source.genres`: Boolean,
  `source.subjects`: Boolean
) extends ImageIncludes

object SingleImageIncludes {
  import ImageInclude._

  def apply(includes: ImageInclude*): SingleImageIncludes =
    SingleImageIncludes(
      withSimilarFeatures = includes.contains(WithSimilarFeatures),
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
  val withSimilarFeatures: Boolean = false
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
