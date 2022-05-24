package weco.api.stacks.models

object CatalogueLocationType {
  // These values mirror the location type IDs from the catalogue pipeline
  // See https://github.com/wellcomecollection/catalogue-pipeline/blob/main/common/internal_model/src/main/scala/weco/catalogue/internal_model/locations/LocationType.scala
  //
  // It only implements the subset of location types used in the API.
  val ClosedStores = "closed-stores"
  val OpenShelves = "open-shelves"
  val OnExhibition = "on-exhibition"
}
