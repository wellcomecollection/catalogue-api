package weco.api.search.models.request

sealed trait SortingOrder

object SortingOrder {
  case object Ascending extends SortingOrder
  case object Descending extends SortingOrder
}
