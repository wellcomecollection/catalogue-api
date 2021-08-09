package weco.api.items.fixtures

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, Uri}
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.locations.{
  AccessCondition,
  LocationType,
  PhysicalLocation
}
import weco.catalogue.internal_model.work.Item
import weco.catalogue.internal_model.work.generators.{
  ItemsGenerators,
  WorkGenerators
}
import weco.sierra.models.identifiers.SierraItemNumber

trait ItemsApiGenerators
    extends WorkGenerators
    with ItemsGenerators {
  def buildEntry(
    sierraItemNumber: SierraItemNumber,
    deleted: String = "false",
    suppressed: String = "false",
    holdCount: Int = 0
  ) = f"""
                        |{
                        |  "id": "${sierraItemNumber.withoutCheckDigit}",
                        |  "deleted": ${deleted},
                        |  "suppressed": ${suppressed},
                        |  "fixedFields": {
                        |    "79": {"label": "LOCATION", "value": "scmwf", "display": "Closed stores A&MSS Well.Found."},
                        |    "88": {"label": "STATUS", "value": "-", "display": "Available"},
                        |    "108": {"label": "OPACMSG", "value": "f", "display": "Online request"}
                        |  },
                        |  "holdCount": ${holdCount}
                        |}
                        |""".stripMargin

  def sierraItemRequest(itemNumber: SierraItemNumber): HttpRequest =
    HttpRequest(
      uri = Uri(
        f"http://sierra:1234/v5/items?id=${itemNumber.withoutCheckDigit}&fields=deleted,fixedFields,holdCount,suppressed"
      )
    )

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
        |  "total": ${entries.size},
        |  "start": 0,
        |  "entries": [
        |    $entries
        |  ]
        |}
        |""".stripMargin
    )
  }

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
