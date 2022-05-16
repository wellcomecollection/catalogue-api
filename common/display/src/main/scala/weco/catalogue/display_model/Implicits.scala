package weco.catalogue.display_model

import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder, HCursor}
import io.circe.syntax._
import weco.http.json.DisplayJsonUtil._
import weco.catalogue.display_model.identifiers.DisplayIdentifier
import weco.catalogue.display_model.languages.DisplayLanguage
import weco.catalogue.display_model.locations._
import weco.catalogue.display_model.work._

object Implicits {

  implicit val locationEncoder: Encoder[DisplayLocation] = {
    case digitalLocation: DisplayDigitalLocation =>
      digitalLocation.asJson
    case physicalLocation: DisplayPhysicalLocation =>
      physicalLocation.asJson
  }

  implicit val locationDecoder: Decoder[DisplayLocation] =
    (c: HCursor) =>
      for {
        ontologyType <- c.downField("type").as[String]

        location <- ontologyType match {
          case "PhysicalLocation" => c.as[DisplayPhysicalLocation]
          case "DigitalLocation"  => c.as[DisplayDigitalLocation]
          case _ =>
            throw new IllegalArgumentException(
              s"Unexpected location type: $ontologyType"
            )
        }
      } yield location

  // Cache these here to improve compilation times (otherwise they are
  // re-derived every time they are required).

  implicit val _enc00: Encoder[DisplayAccessCondition] = deriveConfiguredEncoder
  implicit val _enc02: Encoder[DisplayFormat] = deriveConfiguredEncoder
  implicit val _enc05: Encoder[DisplayIdentifier] = deriveConfiguredEncoder
  implicit val _enc09: Encoder[DisplayItem] = deriveConfiguredEncoder

  implicit val _dec00: Decoder[DisplayAccessCondition] = deriveConfiguredDecoder
  implicit val _dec02: Decoder[DisplayFormat] = deriveConfiguredDecoder
  implicit val _dec05: Decoder[DisplayIdentifier] = deriveConfiguredDecoder
  implicit val _dec09: Decoder[DisplayItem] = deriveConfiguredDecoder
}
