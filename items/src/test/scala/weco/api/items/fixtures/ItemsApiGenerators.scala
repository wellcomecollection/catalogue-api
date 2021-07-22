package weco.api.items.fixtures

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import org.scalatest.Suite
import weco.api.items.services.{ItemUpdateService, SierraItemUpdater}
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.locations.{AccessCondition, LocationType, PhysicalLocation}
import weco.catalogue.internal_model.work.Item
import weco.catalogue.internal_model.work.generators.{ItemsGenerators, WorkGenerators}
import weco.catalogue.source_model.sierra.identifiers.SierraItemNumber
import weco.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait ItemsApiGenerators
    extends SierraServiceFixture
    with WorkGenerators
    with ItemsGenerators {
  this: Suite =>

  def withItemUpdateService[R](
    responses: Seq[(HttpRequest, HttpResponse)] = Seq()
  )(testWith: TestWith[ItemUpdateService, R]): R =
    withMaterializer { implicit mat =>
      withSierraService(responses) { sierraService =>
        val itemsUpdaters = List(
          new SierraItemUpdater(sierraService)
        )

        testWith(new ItemUpdateService(itemsUpdaters))
      }
    }

  def buildEntry(
                  sierraItemNumber: SierraItemNumber,
                  deleted: String = "false",
                  suppressed: String = "false",
                  holdCount: Int = 0
                ) =  f"""
                        |{
                        |  "id": "${sierraItemNumber.withCheckDigit}",
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
