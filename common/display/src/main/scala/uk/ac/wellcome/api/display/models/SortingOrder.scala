package uk.ac.wellcome.api.display.models

sealed trait SortingOrder

object SortingOrder {
  case object Ascending extends SortingOrder
  case object Descending extends SortingOrder
}
