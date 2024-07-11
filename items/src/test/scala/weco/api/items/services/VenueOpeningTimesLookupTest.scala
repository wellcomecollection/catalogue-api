package weco.api.items.services

import akka.http.scaladsl.model._
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.akka.fixtures.Akka
import weco.api.items.fixtures.ItemsApiGenerators
import weco.api.items.models.{ContentApiVenue, OpenClose}
import weco.catalogue.display_model.generators.IdentifiersGenerators
import weco.fixtures.TestWith
import weco.http.client.{HttpGet, MemoryHttpClient}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VenueOpeningTimesLookupTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with Akka
    with ScalaFutures
    with IdentifiersGenerators
    with ItemsApiGenerators {

  def withLookup[R](
    responses: Seq[(HttpRequest, HttpResponse)]
  )(testWith: TestWith[VenuesOpeningTimesLookup, R]): R =
    withActorSystem { implicit actorSystem =>
      val contentApiClient = new MemoryHttpClient(responses) with HttpGet {
        override val baseUri: Uri = Uri("http://content:9002")
      }
      testWith(new VenuesOpeningTimesLookup(contentApiClient))
    }

  it("decodes the content-api venue response into a VenueOpeningTimes") {

    val venueTitle = "name"
    val venueId = "WsuS_R8AACS1Nwlx"

    // Note: this is deliberately a hard-coded JSON string rather than the
    // helpers we use in other tests so we can be sure we really can decode
    // the content API JSON, and avoid any encoding bugs in the tests themselves.
    val responses = Seq(
      (
        contentApiVenueRequest(venueTitle),
        HttpResponse(
          entity = HttpEntity(
            contentType = ContentTypes.`application/json`,
            s"""
              {
                "type": "ResultList",
                "results": [
                  {
                    "type": "Venue",
                    "id": "$venueId",
                    "title": "$venueTitle",
                    "regularOpeningDays": [
                      {
                        "dayOfWeek": "monday",
                        "opens": "10:00",
                        "closes": "18:00",
                        "isClosed": false
                      },
                      {
                        "dayOfWeek": "tuesday",
                        "opens": "10:00",
                        "closes": "18:00",
                        "isClosed": false
                      },
                      {
                        "dayOfWeek": "wednesday",
                        "opens": "10:00",
                        "closes": "18:00",
                        "isClosed": false
                      },
                      {
                        "dayOfWeek": "thursday",
                        "opens": "10:00",
                        "closes": "20:00",
                        "isClosed": false
                      },
                      {
                        "dayOfWeek": "friday",
                        "opens": "10:00",
                        "closes": "18:00",
                        "isClosed": false
                      },
                      {
                        "dayOfWeek": "saturday",
                        "opens": "10:00",
                        "closes": "16:00",
                        "isClosed": false
                      },
                      {
                        "dayOfWeek": "sunday",
                        "opens": "00:00",
                        "closes": "00:00",
                        "isClosed": true
                      }
                    ],
                    "exceptionalClosedDays": [
                      {
                        "overrideDate": "2024-12-24T00:00:00.000Z",
                        "type": "Christmas and New Year",
                        "startDateTime": "00:00",
                        "endDateTime": "00:00"
                      },
                      {
                        "overrideDate": "2024-12-30T00:00:00.000Z",
                        "type": "Christmas and New Year",
                        "startDateTime": "00:00",
                        "endDateTime": "00:00"
                      }
                    ],
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
                        "close": "2024-04-27T15:00:00.000Z"
                      },
                      {
                        "open": "2024-04-29T09:00:00.000Z",
                        "close": "2024-04-29T17:00:00.000Z"
                      }
                    ]
                  }
                ]
              }
          """
          )
        )
      )
    )

    val future = withLookup(responses) {
      _.byVenueName(venueTitle)
    }

    whenReady(future) {
      _ shouldBe Right(
        ContentApiVenue(
          contentType = "Venue",
          id = venueId,
          title = venueTitle,
          openingTimes = List(
            OpenClose("2024-04-24T09:00:00.000Z", "2024-04-24T17:00:00.000Z"),
            OpenClose("2024-04-25T09:00:00.000Z", "2024-04-25T19:00:00.000Z"),
            OpenClose("2024-04-26T09:00:00.000Z", "2024-04-26T17:00:00.000Z"),
            OpenClose("2024-04-27T09:00:00.000Z", "2024-04-27T15:00:00.000Z"),
            OpenClose("2024-04-29T09:00:00.000Z", "2024-04-29T17:00:00.000Z")
          )
        )
      )
    }
  }

  it("returns NotFound if there is no such venue") {
    val venueTitle = "over-the-rainbow"

    val responses = Seq(
      (
        contentApiVenueRequest(venueTitle),
        contentApiVenueErrorResponse(status = StatusCodes.NotFound)
      )
    )

    val future = withLookup(responses) {
      _.byVenueName(venueTitle)
    }

    whenReady(future) {
      _ shouldBe Left(VenueOpeningTimesNotFoundError(venueTitle))
    }
  }

  it("returns Left[UnknownOpeningTimesError] if the API has an error") {
    val venueTitle = "BOOM"

    val responses = Seq(
      (
        contentApiVenueRequest(venueTitle),
        contentApiVenueErrorResponse(status = StatusCodes.InternalServerError)
      )
    )

    val future = withLookup(responses) {
      _.byVenueName(venueTitle)
    }

    whenReady(future) {
      _.left.value shouldBe a[UnknownOpeningTimesError]
    }
  }

  it("wraps an exception in the underlying client") {
    val err = new Throwable("BOOM!")

    val brokenClient = new MemoryHttpClient(responses = Seq()) with HttpGet {
      override val baseUri: Uri = Uri("http://content:9002")

      override def get(
        path: Uri.Path,
        params: Map[String, String]
      ): Future[HttpResponse] =
        Future.failed(err)
    }

    val future = withActorSystem { implicit as =>
      val lookup = new VenuesOpeningTimesLookup(brokenClient)

      lookup.byVenueName("Nice-try")
    }

    whenReady(future) {
      _.left.value shouldBe a[UnknownOpeningTimesError]
    }
  }
}
