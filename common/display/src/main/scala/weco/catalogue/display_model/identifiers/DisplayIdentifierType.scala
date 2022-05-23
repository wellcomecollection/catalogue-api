package weco.catalogue.display_model.identifiers

import io.circe.generic.extras.JsonKey

case class DisplayIdentifierType(
  id: String,
  label: String,
  @JsonKey("type") ontologyType: String = "IdentifierType"
)

case object DisplayIdentifierType {
  // These values mirror the identifier types from the catalogue pipeline
  // See https://github.com/wellcomecollection/catalogue-pipeline/blob/main/common/internal_model/src/main/scala/weco/catalogue/internal_model/identifiers/IdentifierType.scala
  //
  // It only implements the subset of methods used in the API.
  val SierraSystemNumber = DisplayIdentifierType(
    id = "sierra-system-number",
    label = "Sierra system number"
  )
}
