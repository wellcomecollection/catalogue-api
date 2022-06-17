package weco.catalogue.display_model.locations

object CatalogueAccessStatus {
  // These values mirror the access statuses from the catalogue pipeline
  // See https://github.com/wellcomecollection/catalogue-pipeline/blob/main/common/internal_model/src/main/scala/weco/catalogue/internal_model/locations/AccessStatus.scala
  //
  // It only implements the subset of statuses used in the API.
  val Open = DisplayAccessStatus(id = "open", label = "Open")
  val Restricted = DisplayAccessStatus(id = "restricted", label = "Restricted")
  val TemporarilyUnavailable = DisplayAccessStatus(
    id = "temporarily-unavailable",
    label = "Temporarily unavailable"
  )

  val values = Set(
    "open",
    "open-with-advisory",
    "restricted",
    "closed",
    "licensed-resources",
    "unavailable",
    "permission-required"
  )
}
