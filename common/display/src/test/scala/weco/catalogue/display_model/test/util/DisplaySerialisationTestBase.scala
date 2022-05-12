package weco.catalogue.display_model.test.util

import io.circe.Json
import org.scalatest.Suite
import weco.catalogue.display_model.locations.{
  DisplayAccessMethod,
  DisplayAccessStatus,
  DisplayLocationType
}
import weco.catalogue.internal_model.identifiers.SourceIdentifier
import weco.catalogue.internal_model.locations._
import weco.json.JsonUtil._

import scala.util.{Failure, Success}

trait DisplaySerialisationTestBase {
  this: Suite =>

  implicit class JsonStringOps(s: String) {
    def tidy: String = {
      val tidiedFields =
        s
        // Replace anything that looks like '"key": None,' in the output.
          .replaceAll(""""[a-zA-Z]+": None,""".stripMargin, "")
          // Unwrap anything that looks like '"key": Some({…})' in the output
          .replaceAll("""Some\(\{(.*)\}\)""", "{$1}")
          // Unwrap anything that looks like '"key": Some("…")' in the output
          .replaceAll("""Some\("(.*)"\)""", "\"$1\"")

      fromJson[Json](tidiedFields) match {
        case Success(j) => j.noSpaces
        case Failure(_) =>
          throw new IllegalArgumentException(
            s"Unable to parse JSON:\n$tidiedFields"
          )
      }
    }

    def toJson: String =
      Json.fromString(s).noSpaces
  }

  def locations(locations: List[Location]) =
    locations.map(location).mkString(",")

  def location(loc: Location) =
    loc match {
      case l: DigitalLocation  => digitalLocation(l)
      case l: PhysicalLocation => physicalLocation(l)
    }

  def digitalLocation(loc: DigitalLocation): String =
    s"""{
      "type": "DigitalLocation",
      "locationType": ${locationType(loc.locationType)},
      "url": "${loc.url}",
      "license": ${loc.license.map(license)},
      "credit": ${loc.credit.map(_.toJson)},
      "linkText": ${loc.linkText.map(_.toJson)},
      "accessConditions": ${accessConditions(loc.accessConditions)}
    }""".tidy

  def physicalLocation(loc: PhysicalLocation): String =
    s"""
       {
        "type": "PhysicalLocation",
        "locationType": ${locationType(loc.locationType)},
        "label": "${loc.label}",
        "license": ${loc.license.map(license)},
        "shelfmark": ${loc.shelfmark.map(_.toJson)},
        "accessConditions": ${accessConditions(loc.accessConditions)}
       }
     """.tidy

  private def accessConditions(conds: List[AccessCondition]) =
    s"[${conds.map(accessCondition).mkString(",")}]"

  private def accessCondition(cond: AccessCondition): String =
    s"""
      {
        "method": {
          "type": "AccessMethod",
          "id": "${DisplayAccessMethod(cond.method).id}",
          "label": "${DisplayAccessMethod(cond.method).label}"
        },
        "terms": ${cond.terms.map(_.toJson)},
        "status": ${cond.status.map(accessStatus)},
        "type": "AccessCondition"
      }
    """.tidy

  private def accessStatus(status: AccessStatus): String =
    s"""{
       |  "type": "AccessStatus",
       |  "id": ${DisplayAccessStatus(status).id.toJson},
       |  "label": ${DisplayAccessStatus(status).label.toJson}
       |}
       |""".stripMargin.tidy

  private def license(license: License): String =
    s"""{
      "id": "${license.id}",
      "label": "${license.label}",
      "url": "${license.url}",
      "type": "License"
    }""".tidy

  def identifier(identifier: SourceIdentifier) =
    s"""{
      "type": "Identifier",
      "identifierType": {
        "id": "${identifier.identifierType.id}",
        "label": "${identifier.identifierType.label}",
        "type": "IdentifierType"
      },
      "value": "${identifier.value}"
    }"""

  private def locationType(locType: LocationType): String =
    s"""{
         "id": "${DisplayLocationType(locType).id}",
         "label": "${DisplayLocationType(locType).label}",
         "type": "LocationType"
       }
     """ stripMargin
}
