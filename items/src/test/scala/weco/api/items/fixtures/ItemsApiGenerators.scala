package weco.api.items.fixtures

import akka.http.scaladsl.model._
import weco.catalogue.display_model.models.{DisplayWork, WorksIncludes}
import weco.catalogue.internal_model.identifiers.{CanonicalId, IdState}
import weco.catalogue.internal_model.locations.{
  AccessCondition,
  LocationType,
  PhysicalLocation
}
import weco.catalogue.internal_model.work.generators.{
  ItemsGenerators,
  WorkGenerators
}
import weco.catalogue.internal_model.work.{Item, Work, WorkState}
import weco.http.json.DisplayJsonUtil
import weco.http.json.DisplayJsonUtil._
import weco.http.models.DisplayError
import weco.sierra.http.SierraSource
import weco.sierra.models.identifiers.SierraItemNumber

trait ItemsApiGenerators extends WorkGenerators with ItemsGenerators {
  def buildEntry(
    sierraItemNumber: SierraItemNumber,
    deleted: String = "false",
    suppressed: String = "false",
    holdCount: Int = 0
  ) = f"""
                        |{
                        |  "id": "${sierraItemNumber.withoutCheckDigit}",
                        |  "deleted": $deleted,
                        |  "suppressed": $suppressed,
                        |  "fixedFields": {
                        |    "79": {"label": "LOCATION", "value": "scmwf", "display": "Closed stores A&MSS Well.Found."},
                        |    "88": {"label": "STATUS", "value": "-", "display": "Available"},
                        |    "108": {"label": "OPACMSG", "value": "f", "display": "Online request"}
                        |  },
                        |  "holdCount": $holdCount
                        |}
                        |""".stripMargin

  def sierraItemRequest(itemNumber: SierraItemNumber): HttpRequest = {
    val fieldList = SierraSource.requiredItemFields.mkString(",")

    HttpRequest(
      uri = Uri(
        f"http://sierra:1234/v5/items?id=${itemNumber.withoutCheckDigit}&fields=$fieldList"
      )
    )
  }

  def sierraItemResponse(
    sierraItemNumber: SierraItemNumber,
    deleted: String = "false",
    suppressed: String = "false",
    holdCount: Int = 0
  ): HttpEntity.Strict = {

    val entries = Seq(
      buildEntry(sierraItemNumber, deleted, suppressed, holdCount)
    ).mkString(",\n")

    HttpEntity(
      contentType = ContentTypes.`application/json`,
      f"""
        |{
        |  "total": ${entries.length},
        |  "start": 0,
        |  "entries": [
        |    $entries
        |  ]
        |}
        |""".stripMargin
    )
  }

  def catalogueWorkRequest(id: CanonicalId): HttpRequest =
    HttpRequest(
      uri = Uri(s"http://catalogue:9001/works/$id?include=identifiers,items")
    )

  // This is to make sure our locations look like locations from the catalogue API,
  // and not some other serialisation, e.g.
  //
  //    "locations": [ { "displayPhysicalLocation": { … } }, … ]
  //
  import weco.catalogue.display_model.models.Implicits._

  def catalogueWorkResponse(
    work: Work.Visible[WorkState.Indexed]
  ): HttpResponse =
    HttpResponse(
      entity = HttpEntity(
        contentType = ContentTypes.`application/json`,
        DisplayJsonUtil.toJson(DisplayWork(work, includes = WorksIncludes.all))
      )
    )

  def catalogueErrorResponse(status: StatusCode): HttpResponse =
    HttpResponse(
      status = status,
      entity = HttpEntity(
        contentType = ContentTypes.`application/json`,
        DisplayJsonUtil.toJson(DisplayError(statusCode = status))
      )
    )

  def createPhysicalItemWith(
    sierraItemNumber: SierraItemNumber,
    accessCondition: AccessCondition
  ): Item[IdState.Identified] = {

    val physicalItemLocation: PhysicalLocation = createPhysicalLocationWith(
      accessConditions = List(accessCondition),
      locationType = LocationType.ClosedStores,
      license = None,
      shelfmark = None
    )

    val itemSourceIdentifier = createSierraSystemSourceIdentifierWith(
      value = sierraItemNumber.withCheckDigit,
      ontologyType = "Item"
    )

    createIdentifiedItemWith(
      sourceIdentifier = itemSourceIdentifier,
      locations = List(physicalItemLocation)
    )
  }
}
