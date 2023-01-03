package weco.api.search.models.request

sealed trait WorkInclude

object WorkInclude {
  case object Identifiers extends WorkInclude
  case object Items extends WorkInclude
  case object Holdings extends WorkInclude
  case object Subjects extends WorkInclude
  case object Genres extends WorkInclude
  case object Contributors extends WorkInclude
  case object Production extends WorkInclude
  case object Languages extends WorkInclude
  case object Notes extends WorkInclude
  case object FormerFrequency extends WorkInclude
  case object Designation extends WorkInclude
  case object Images extends WorkInclude
  case object Parts extends WorkInclude
  case object PartOf extends WorkInclude
  case object PrecededBy extends WorkInclude
  case object SucceededBy extends WorkInclude
}

case class WorksIncludes(
  identifiers: Boolean,
  items: Boolean,
  holdings: Boolean,
  subjects: Boolean,
  genres: Boolean,
  contributors: Boolean,
  production: Boolean,
  languages: Boolean,
  notes: Boolean,
  formerFrequency: Boolean,
  designation: Boolean,
  images: Boolean,
  parts: Boolean,
  partOf: Boolean,
  precededBy: Boolean,
  succeededBy: Boolean
)

case object WorksIncludes {
  def apply(includes: WorkInclude*): WorksIncludes =
    WorksIncludes(
      identifiers = includes.contains(WorkInclude.Identifiers),
      items = includes.contains(WorkInclude.Items),
      holdings = includes.contains(WorkInclude.Holdings),
      subjects = includes.contains(WorkInclude.Subjects),
      genres = includes.contains(WorkInclude.Genres),
      contributors = includes.contains(WorkInclude.Contributors),
      production = includes.contains(WorkInclude.Production),
      languages = includes.contains(WorkInclude.Languages),
      notes = includes.contains(WorkInclude.Notes),
      formerFrequency = includes.contains(WorkInclude.FormerFrequency),
      designation = includes.contains(WorkInclude.Designation),
      images = includes.contains(WorkInclude.Images),
      parts = includes.contains(WorkInclude.Parts),
      partOf = includes.contains(WorkInclude.PartOf),
      precededBy = includes.contains(WorkInclude.PrecededBy),
      succeededBy = includes.contains(WorkInclude.SucceededBy)
    )

  def none: WorksIncludes = WorksIncludes()
}
