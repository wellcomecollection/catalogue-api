package uk.ac.wellcome.api.display.models

sealed trait SortRequest

case object ProductionDateSortRequest extends SortRequest
