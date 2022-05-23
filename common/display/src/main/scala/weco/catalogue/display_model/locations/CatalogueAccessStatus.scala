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

  // This is a mapping of IDs to the name used in the Elasticsearch index.  This is
  // because they're encoded in a slightly awkward way:
  //
  //      "accessConditions" : [
  //        {
  //          "status" : { "type" : "Open" }
  //        }
  //      ]
  //
  // We might want to change this at some point, but until then then API has to be
  // aware of these names.
  val indexValues = Map(
    "open" -> "Open",
    "open-with-advisory" -> "OpenWithAdvisory",
    "restricted" -> "Restricted",
    "closed" -> "Closed",
    "licensed-resources" -> "LicensedResources",
    "unavailable" -> "Unavailable",
    "permission-required" -> "PermissionRequired"
  )
}
