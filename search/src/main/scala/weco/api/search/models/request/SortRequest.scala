package weco.api.search.models.request

sealed trait SortRequest

case object ProductionDateSortRequest extends SortRequest
