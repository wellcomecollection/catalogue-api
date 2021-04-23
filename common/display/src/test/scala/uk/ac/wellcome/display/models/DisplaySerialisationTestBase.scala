package uk.ac.wellcome.display.models

import org.scalatest.Suite
import uk.ac.wellcome.json.JsonUtil._
import weco.catalogue.internal_model.identifiers.{
  HasId,
  IdState,
  SourceIdentifier
}
import weco.catalogue.internal_model.image.ImageData
import weco.catalogue.internal_model.languages.Language
import weco.catalogue.internal_model.locations._
import weco.catalogue.internal_model.work._

trait DisplaySerialisationTestBase {
  this: Suite =>

  def optionalString(fieldName: String,
                     maybeStringValue: Option[String],
                     trailingComma: Boolean = true): String =
    maybeStringValue match {
      case None => ""
      case Some(value) =>
        s"""
          "$fieldName": "$value"
          ${if (trailingComma) "," else ""}
        """
    }

  def optionalObject[T](fieldName: String,
                        formatter: T => String,
                        maybeObjectValue: Option[T],
                        firstField: Boolean = false) =
    maybeObjectValue match {
      case None => ""
      case Some(o) =>
        s"""
           ${if (!firstField) ","}"$fieldName": ${formatter(o)}
         """
    }

  def items(items: List[Item[IdState.Minted]]) =
    items.map(item).mkString(",")

  def item(item: Item[IdState.Minted]) =
    s"""
     {
       ${identifiers(item)}
       "type": "Item",
       ${optionalString("title", item.title)}
       "locations": [${locations(item.locations)}]
     }
    """

  def locations(locations: List[Location]) =
    locations.map(location).mkString(",")

  def location(loc: Location) =
    loc match {
      case l: DigitalLocation  => digitalLocation(l)
      case l: PhysicalLocation => physicalLocation(l)
    }

  def digitalLocation(digitalLocation: DigitalLocation) =
    s"""{
      "type": "DigitalLocation",
      "locationType": ${locationType(digitalLocation.locationType)},
      "url": "${digitalLocation.url}"
      ${optionalObject("license", license, digitalLocation.license)},
      ${optionalString("credit", digitalLocation.credit)}
      ${optionalString("linkText", digitalLocation.linkText)}
      "accessConditions": ${accessConditions(digitalLocation.accessConditions)}
    }"""

  def physicalLocation(loc: PhysicalLocation) =
    s"""
       {
        "type": "PhysicalLocation",
        "locationType": ${locationType(loc.locationType)},
        "label": "${loc.label}"
        ${optionalObject("license", license, loc.license)},
        ${optionalString("shelfmark", loc.shelfmark)}
        "accessConditions": ${accessConditions(loc.accessConditions)}
       }
     """

  def accessConditions(conds: List[AccessCondition]) =
    s"[${conds.map(accessCondition).mkString(",")}]"

  def accessCondition(cond: AccessCondition) =
    s"""
      {
        "type": "AccessCondition",
        ${optionalString("terms", cond.terms)}
        ${optionalString("to", cond.to, trailingComma = false)}
        ${optionalObject("status", accessStatus, cond.status)}
      }
    """

  def accessStatus(status: AccessStatus) = {
    s"""
      {
        "type": "AccessStatus",
        "id": "${DisplayAccessStatus(status).id}",
        "label": "${DisplayAccessStatus(status).label}"
      }
    """
  }
  def identifiers(obj: HasId[IdState.Minted]) =
    obj.id match {
      case IdState.Identified(canonicalId, _, _) => s"""
        "id": "$canonicalId",
      """
      case IdState.Unidentifiable                => ""
    }

  def abstractAgent(ag: AbstractAgent[IdState.Minted]) =
    ag match {
      case a: Agent[IdState.Minted]        => agent(a)
      case o: Organisation[IdState.Minted] => organisation(o)
      case p: Person[IdState.Minted]       => person(p)
      case m: Meeting[IdState.Minted]      => meeting(m)
    }

  def person(person: Person[IdState.Minted]) =
    s"""{
       ${identifiers(person)}
        "type": "Person",
        ${optionalString("prefix", person.prefix)}
        ${optionalString("numeration", person.numeration)}
        "label": "${person.label}"
      }"""

  def organisation(organisation: Organisation[IdState.Minted]) =
    s"""{
       ${identifiers(organisation)}
        "type": "Organisation",
        "label": "${organisation.label}"
      }"""

  def meeting(meeting: Meeting[IdState.Minted]) =
    s"""{
       ${identifiers(meeting)}
        "type": "Meeting",
        "label": "${meeting.label}"
      }"""

  def agent(agent: Agent[IdState.Minted]) =
    s"""{
       ${identifiers(agent)}
        "type": "Agent",
        "label": "${agent.label}"
      }"""

  def period(period: Period[IdState.Minted]) =
    s"""{
       ${identifiers(period)}
      "type": "Period",
      "label": "${period.label}"
    }"""

  def place(place: Place[IdState.Minted]) =
    s"""{
       ${identifiers(place)}
      "type": "Place",
      "label": "${place.label}"
    }"""

  def concept(concept: Concept[IdState.Minted]) =
    s"""{
       ${identifiers(concept)}
      "type": "Concept",
      "label": "${concept.label}"
    }"""

  def abstractRootConcept(
    abstractRootConcept: AbstractRootConcept[IdState.Minted]) =
    abstractRootConcept match {
      case c: Concept[IdState.Minted]      => concept(c)
      case p: Place[IdState.Minted]        => place(p)
      case p: Period[IdState.Minted]       => period(p)
      case a: Agent[IdState.Minted]        => agent(a)
      case o: Organisation[IdState.Minted] => organisation(o)
      case p: Person[IdState.Minted]       => person(p)
      case m: Meeting[IdState.Minted]      => meeting(m)
    }

  def concepts(concepts: List[AbstractRootConcept[IdState.Minted]]) =
    concepts.map(abstractRootConcept).mkString(",")

  def subject(s: Subject[IdState.Minted],
              showConcepts: Boolean = true): String =
    s"""
    {
      "label": "${s.label}",
      "type" : "Subject",
      "concepts": [ ${if (showConcepts) concepts(s.concepts) else ""} ]
    }
    """

  def subjects(subjects: List[Subject[IdState.Minted]]): String =
    subjects.map(subject(_)).mkString(",")

  def genre(genre: Genre[IdState.Minted]) =
    s"""
    {
      "label": "${genre.label}",
      "type" : "Genre",
      "concepts": [ ${concepts(genre.concepts)} ]
    }
    """

  def genres(genres: List[Genre[IdState.Minted]]) =
    genres.map(genre).mkString(",")

  def contributor(contributor: Contributor[IdState.Minted]) =
    s"""
      {
        ${identifiers(contributor)}
        "agent": ${abstractAgent(contributor.agent)},
        "roles": ${toJson(contributor.roles).get},
        "type": "Contributor"
      }
    """

  def contributors(contributors: List[Contributor[IdState.Minted]]) =
    contributors.map(contributor).mkString(",")

  def production(production: List[ProductionEvent[IdState.Minted]]) =
    production.map(productionEvent).mkString(",")

  def availabilities(availabilities: Set[Availability]) =
    availabilities.map(availability).mkString(",")

  def languages(ls: List[Language]): String =
    ls.map(language).mkString(",")

  def workImageInclude(image: ImageData[IdState.Identified]) =
    s"""
       {
         "id": "${image.id.canonicalId}",
         "type": "Image"
       }
    """

  def workImageIncludes(images: List[ImageData[IdState.Identified]]) =
    images.map(workImageInclude).mkString(",")

  def availability(availability: Availability): String =
    s"""
      {
        "id": "${availability.id}",
        "label": "${availability.label}",
        "type": "Availability"
      }
     """.stripMargin

  def productionEvent(event: ProductionEvent[IdState.Minted]): String =
    s"""
      {
        "label": "${event.label}",
        "dates": [${event.dates.map(period).mkString(",")}],
        "agents": [${event.agents.map(abstractAgent).mkString(",")}],
        "places": [${event.places.map(place).mkString(",")}],
        "type": "ProductionEvent"
      }
    """

  def format(fmt: Format): String =
    s"""
      {
        "id": "${fmt.id}",
        "label": "${fmt.label}",
        "type": "Format"
      }
    """

  def language(lang: Language): String =
    s"""
       {
         "id": "${lang.id}",
         "label": "${lang.label}",
         "type": "Language"
       }
     """

  def license(license: License) =
    s"""{
      "id": "${license.id}",
      "label": "${license.label}",
      "url": "${license.url}",
      "type": "License"
    }"""

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

  def locationType(locType: LocationType): String =
    s"""{
         "id": "${DisplayLocationType(locType).id}",
         "label": "${DisplayLocationType(locType).label}",
         "type": "LocationType"
       }
     """ stripMargin

  def singleHoldings(h: Holdings): String =
    s"""
       |{
       |  ${optionalString("note", h.note)}
       |  "enumeration": [
       |    ${h.enumeration
         .map { en =>
           '"' + en + '"'
         }
         .mkString(",")}
       |  ]
       |  ${optionalObject("location", location, h.location)},
       |  "type": "Holdings"
       |}
       |""".stripMargin

  def listOfHoldings(hs: List[Holdings]): String =
    hs.map { singleHoldings }.mkString(",")
}
