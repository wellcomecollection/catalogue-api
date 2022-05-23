package weco.api.stacks.models

import weco.catalogue.display_model.locations.DisplayAccessMethod

object CatalogueAccessMethod {
  // These values mirror the access methods from the catalogue pipeline
  // See https://github.com/wellcomecollection/catalogue-pipeline/blob/main/common/internal_model/src/main/scala/weco/catalogue/internal_model/locations/AccessMethod.scala
  //
  // It only implements the subset of methods used in the API.
  val OnlineRequest = DisplayAccessMethod(
    id = "online-request",
    label = "Online request"
  )

  val NotRequestable = DisplayAccessMethod(
    id = "not-requestable",
    label = "Not requestable"
  )
}
