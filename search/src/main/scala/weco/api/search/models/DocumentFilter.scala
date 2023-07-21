package weco.api.search.models

import java.time.LocalDate
sealed trait DocumentFilter
sealed trait WorkFilter extends DocumentFilter

sealed trait ImageFilter extends DocumentFilter

sealed trait PairableFilter
case class ItemLocationTypeIdFilter(locationTypeIds: Seq[String])
    extends WorkFilter

case class FormatFilter(formatIds: Seq[String])
    extends WorkFilter
    with PairableFilter

case class WorkTypeFilter(types: Seq[String]) extends WorkFilter

case class DateRangeFilter(
  fromDate: Option[LocalDate],
  toDate: Option[LocalDate]
) extends WorkFilter
    with ImageFilter

case object VisibleWorkFilter extends WorkFilter

case class LanguagesFilter(languageIds: Seq[String])
    extends WorkFilter
    with PairableFilter

case class GenreFilter(genreQuery: Seq[String])
    extends WorkFilter
    with ImageFilter
    with PairableFilter

case class GenreConceptFilter(conceptIds: Seq[String])
    extends WorkFilter
    with ImageFilter

case class SubjectLabelFilter(labels: Seq[String])
    extends WorkFilter
    with ImageFilter
    with PairableFilter

case class ContributorsFilter(contributorQueries: Seq[String])
    extends WorkFilter
    with ImageFilter
    with PairableFilter

case class LicenseFilter(licenseIds: Seq[String])
    extends WorkFilter
    with ImageFilter
    with PairableFilter

case class IdentifiersFilter(values: Seq[String]) extends WorkFilter

case class ItemsFilter(values: Seq[String]) extends WorkFilter
case class ItemsIdentifiersFilter(values: Seq[String]) extends WorkFilter

case class AccessStatusFilter(
  includes: List[String],
  excludes: List[String]
) extends WorkFilter

case class PartOfFilter(id: String) extends WorkFilter
case class PartOfTitleFilter(id: String) extends WorkFilter

case class AvailabilitiesFilter(availabilityIds: Seq[String])
    extends WorkFilter
    with PairableFilter
