package weco.catalogue.display_model.locations

import io.circe.generic.extras.JsonKey

sealed trait DisplayLocation {
  val accessConditions: List[DisplayAccessCondition]
}

case class DisplayDigitalLocation(
  locationType: DisplayLocationType,
  url: String,
  credit: Option[String] = None,
  linkText: Option[String] = None,
  license: Option[DisplayLicense] = None,
  accessConditions: List[DisplayAccessCondition] = Nil,
  @JsonKey("type") ontologyType: String = "DigitalLocation"
) extends DisplayLocation

case class DisplayPhysicalLocation(
  locationType: DisplayLocationType,
  label: String,
  license: Option[DisplayLicense] = None,
  shelfmark: Option[String] = None,
  accessConditions: List[DisplayAccessCondition] = Nil,
  @JsonKey("type") ontologyType: String = "PhysicalLocation"
) extends DisplayLocation
