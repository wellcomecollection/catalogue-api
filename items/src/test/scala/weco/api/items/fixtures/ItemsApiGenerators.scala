package weco.api.items.fixtures

import org.apache.pekko.http.scaladsl.model._
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
    holdCount: Int = 0,
    locationCode: String = "some-location-code"
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
                        |  "holdCount": $holdCount,
                        |  "location": {"code": "$locationCode", "name": "some-location-name"}
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
    holdCount: Int = 0,
    locationCode: String = "some-location-code"
  ): HttpEntity.Strict = {

    val entries = Seq(
      buildEntry(sierraItemNumber, deleted, suppressed, holdCount, locationCode)
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

  def contentApiVenueRequest(venueName: String): HttpRequest =
    HttpRequest(
      uri = Uri(s"http://content:9002/venues?title=library,$venueName")
    )

  def contentApiVenueResponse(): HttpResponse =
    HttpResponse(
      entity = HttpEntity(
        contentType = ContentTypes.`application/json`,
        s"""
          {
            "type": "ResultList",
            "results": [
              {
                "type": "Venue",
                "id": "venue-id",
                "title": "library",
                "nextOpeningDates": [
                  {
                    "open": "2024-04-24T09:00:00.000Z",
                    "close": "2024-04-24T17:00:00.000Z"
                  },
                  {
                    "open": "2024-04-25T09:00:00.000Z",
                    "close": "2024-04-25T19:00:00.000Z"
                  },
                  {
                    "open": "2024-04-26T09:00:00.000Z",
                    "close": "2024-04-26T17:00:00.000Z"
                  }
                ]
              }
            ]
          }
          """
      )
    )

  def contentApiVenueResponse(venueName: String): HttpResponse =
    HttpResponse(
      entity = HttpEntity(
        contentType = ContentTypes.`application/json`,
        s"""
          {
            "type": "ResultList",
            "results": [
              {
                "type": "Venue",
                "id": "venue-id",
                "title": "library",
                "nextOpeningDates": [
                  {
                    "open": "2024-04-24T09:00:00.000Z",
                    "close": "2024-04-24T17:00:00.000Z"
                  },
                  {
                    "open": "2024-04-25T09:00:00.000Z",
                    "close": "2024-04-25T19:00:00.000Z"
                  },
                  {
                    "open": "2024-04-26T09:00:00.000Z",
                    "close": "2024-04-26T17:00:00.000Z"
                  },
                  {
                    "open": "2024-04-27T09:00:00.000Z",
                    "close": "2024-04-27T17:00:00.000Z"
                  },
                  {
                    "open": "2024-04-28T09:00:00.000Z",
                    "close": "2024-04-28T17:00:00.000Z"
                  },
                  {
                    "open": "2024-04-29T09:00:00.000Z",
                    "close": "2024-04-29T17:00:00.000Z"
                  },
                  {
                    "open": "2024-04-30T09:00:00.000Z",
                    "close": "2024-04-30T17:00:00.000Z"
                  },
                  {
                    "open": "2024-05-01T09:00:00.000Z",
                    "close": "2024-05-01T17:00:00.000Z"
                  },
                  {
                    "open": "2024-05-02T09:00:00.000Z",
                    "close": "2024-05-02T17:00:00.000Z"
                  },
                  {
                    "open": "2024-05-03T09:00:00.000Z",
                    "close": "2024-05-03T17:00:00.000Z"
                  },
                  {
                    "open": "2024-05-04T09:00:00.000Z",
                    "close": "2024-05-04T17:00:00.000Z"
                  },
                  {
                    "open": "2024-05-05T09:00:00.000Z",
                    "close": "2024-05-05T17:00:00.000Z"
                  }
                ]
              },
             {
                "type": "Venue",
                "id": "venue-id",
                "title": "$venueName",
                "nextOpeningDates": [
                  {
                    "open": "2024-04-24T09:00:00.000Z",
                    "close": "2024-04-24T17:00:00.000Z"
                  },
                  {
                    "open": "2024-04-25T09:00:00.000Z",
                    "close": "2024-04-25T19:00:00.000Z"
                  },
                  {
                    "open": "2024-04-26T09:00:00.000Z",
                    "close": "2024-04-26T17:00:00.000Z"
                  },
                  {
                    "open": "2024-04-27T09:00:00.000Z",
                    "close": "2024-04-27T17:00:00.000Z"
                  },
                  {
                    "open": "2024-04-28T09:00:00.000Z",
                    "close": "2024-04-28T17:00:00.000Z"
                  },
                  {
                    "open": "2024-04-29T09:00:00.000Z",
                    "close": "2024-04-29T17:00:00.000Z"
                  },
                  {
                    "open": "2024-04-30T09:00:00.000Z",
                    "close": "2024-04-30T17:00:00.000Z"
                  },
                  {
                    "open": "2024-05-01T09:00:00.000Z",
                    "close": "2024-05-01T17:00:00.000Z"
                  },
                  {
                    "open": "2024-05-02T09:00:00.000Z",
                    "close": "2024-05-02T17:00:00.000Z"
                  },
                  {
                    "open": "2024-05-03T09:00:00.000Z",
                    "close": "2024-05-03T17:00:00.000Z"
                  },
                  {
                    "open": "2024-05-04T09:00:00.000Z",
                    "close": "2024-05-04T17:00:00.000Z"
                  }
                ]
              }
            ]
          }
          """
      )
    )

  def contentApiVenueErrorResponse(status: StatusCode): HttpResponse =
    HttpResponse(
      status = status,
      entity = HttpEntity(
        contentType = ContentTypes.`application/json`,
        DisplayJsonUtil.toJson(DisplayError(statusCode = status))
      )
    )
}
