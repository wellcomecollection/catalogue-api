package uk.ac.wellcome.platform.api.models

import java.time.LocalDate

sealed trait WorkFilter

case class ItemLocationTypeFilter(locationTypeIds: Seq[String])
    extends WorkFilter

case object ItemLocationTypeFilter {
  def apply(locationTypeId: String): ItemLocationTypeFilter =
    ItemLocationTypeFilter(locationTypeIds = Seq(locationTypeId))
}

case class WorkTypeFilter(workTypeIds: Seq[String]) extends WorkFilter

case object WorkTypeFilter {
  def apply(workTypeId: String): WorkTypeFilter =
    WorkTypeFilter(workTypeIds = Seq(workTypeId))
}

case class DateRangeFilter(fromDate: Option[LocalDate],
                           toDate: Option[LocalDate])
    extends WorkFilter

case object IdentifiedWorkFilter extends WorkFilter
