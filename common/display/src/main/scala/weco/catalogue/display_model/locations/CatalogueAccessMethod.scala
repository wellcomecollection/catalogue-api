package weco.catalogue.display_model.locations

object CatalogueAccessMethod {
  // These values mirror the access methods from the catalogue pipeline
  // See https://github.com/wellcomecollection/catalogue-pipeline/blob/main/common/internal_model/src/main/scala/weco/catalogue/internal_model/locations/AccessMethod.scala
  //
  // Only the methods the API needs to name are given as id/label pairs below
  val OnlineRequest = DisplayAccessMethod(
    id = "online-request",
    label = "Online request"
  )

  val NotRequestable = DisplayAccessMethod(
    id = "not-requestable",
    label = "Not requestable"
  )

  val values = Seq(
    "online-request",
    "manual-request",
    "not-requestable",
    "view-online",
    "open-shelves"
  )
}
