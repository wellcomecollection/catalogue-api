package weco.api.items.fixtures

import akka.http.scaladsl.model._
import weco.fixtures.LocalResources
import weco.http.json.DisplayJsonUtil
import weco.http.models.DisplayError
import weco.sierra.http.SierraSource
import weco.sierra.models.identifiers.SierraItemNumber

trait ItemsApiGenerators extends LocalResources {
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

  def catalogueWorkRequest(id: String): HttpRequest =
    HttpRequest(
      uri = Uri(s"http://catalogue:9001/works/$id?include=identifiers,items")
    )

  def catalogueWorkResponse(resourceName: String): HttpResponse =
    HttpResponse(
      entity = HttpEntity(
        contentType = ContentTypes.`application/json`,
        readResource(resourceName)
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
}
