package weco.api.items.services

import org.apache.pekko.http.scaladsl.model._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import weco.api.items.fixtures.ItemsApiGenerators
import weco.api.stacks.models.{CatalogueAccessMethod, CatalogueWork}
import weco.catalogue.display_model.generators.IdentifiersGenerators
import weco.catalogue.display_model.identifiers.{
  DisplayIdentifier,
  DisplayIdentifierType
}
import weco.catalogue.display_model.locations._
import weco.catalogue.display_model.work.{AvailabilitySlot, DisplayItem}
import weco.fixtures.TestWith
import weco.http.client.{HttpGet, MemoryHttpClient}
import weco.json.utils.JsonAssertions
import weco.sierra.fixtures.SierraSourceFixture
import weco.sierra.generators.SierraIdentifierGenerators
import weco.sierra.models.identifiers.SierraItemNumber

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SierraItemUpdaterTest
    extends AnyFunSpec
    with Matchers
    with JsonAssertions
    with ItemsApiGenerators
    with SierraSourceFixture
    with IdentifiersGenerators
    with SierraIdentifierGenerators
    with IntegrationPatience {

  def withSierraItemUpdater[R](
    sierraResponses: Seq[(HttpRequest, HttpResponse)] = Seq(),
    contentApiResponses: Seq[(HttpRequest, HttpResponse)] = Seq(),
    clock: Clock
  )(testWith: TestWith[ItemUpdater, R]): R =
    withActorSystem { implicit actorSystem =>
      withSierraSource(sierraResponses) { sierraSource =>
        val contentApiClient = new MemoryHttpClient(contentApiResponses)
        with HttpGet {
          override val baseUri: Uri = Uri("http://content:9002")
        }

        testWith(
          new SierraItemUpdater(
            sierraSource,
            new VenuesOpeningTimesLookup(contentApiClient),
            clock
          )
        )
      }
    }

  def withClock[R](
    dateTime: String = "2024-04-24T09:00:00.000Z"
  )(testWith: TestWith[Clock, R]): R = {
    val instant = Instant.parse(dateTime)
    val zoneId = ZoneId.of("Europe/London")
    testWith(Clock.fixed(instant, zoneId))
  }

  def createSierraSystemSourceIdentifier: DisplayIdentifier =
    DisplayIdentifier(
      identifierType = DisplayIdentifierType.SierraSystemNumber,
      value = randomAlphanumeric()
    )

  def missingItemResponse(
    sierraItemNumber: SierraItemNumber
  ): Seq[(HttpRequest, HttpResponse)] = Seq(
    (
      sierraItemRequest(sierraItemNumber),
      HttpResponse(
        status = StatusCodes.NotFound,
        entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          """
            |{
            |  "code": 107,
            |  "specificCode": 0,
            |  "httpStatus": 404,
            |  "name": "Record not found"
            |}
            |""".stripMargin
        )
      )
    )
  )

  def availableItemResponses(
    sierraItemNumber: SierraItemNumber
  ): Seq[(HttpRequest, HttpResponse)] = Seq(
    (
      sierraItemRequest(sierraItemNumber),
      HttpResponse(
        entity = sierraItemResponse(
          sierraItemNumber = sierraItemNumber
        )
      )
    )
  )

  def availableDeepstoreItemResponse(
    sierraItemNumber: SierraItemNumber
  ): Seq[(HttpRequest, HttpResponse)] = Seq(
    (
      sierraItemRequest(sierraItemNumber),
      HttpResponse(
        entity = sierraItemResponse(
          sierraItemNumber = sierraItemNumber,
          locationCode = "harop"
        )
      )
    )
  )

  def onHoldItemResponses(
    sierraItemNumber: SierraItemNumber
  ): Seq[(HttpRequest, HttpResponse)] = Seq(
    (
      sierraItemRequest(sierraItemNumber),
      HttpResponse(
        entity = sierraItemResponse(
          sierraItemNumber = sierraItemNumber,
          holdCount = 1
        )
      )
    )
  )

  def temporarilyUnavailableItem(
    sierraItemNumber: SierraItemNumber
  ): DisplayItem = {
    val temporarilyUnavailableOnline = DisplayAccessCondition(
      method = CatalogueAccessMethod.NotRequestable,
      status = CatalogueAccessStatus.TemporarilyUnavailable
    )

    createPhysicalItemWith(
      sierraItemNumber = sierraItemNumber,
      accessCondition = temporarilyUnavailableOnline
    )
  }

  def availableItem(sierraItemNumber: SierraItemNumber) =
    createPhysicalItemWith(
      sierraItemNumber = sierraItemNumber,
      accessCondition = availableOnlineAccessCondition
    )

  val availableOnlineAccessCondition = DisplayAccessCondition(
    method = CatalogueAccessMethod.OnlineRequest
  )

  val onHoldAccessCondition = DisplayAccessCondition(
    method = CatalogueAccessMethod.NotRequestable,
    status = Some(CatalogueAccessStatus.TemporarilyUnavailable),
    note = Some(
      "Item is in use by another reader. Please ask at Library Enquiry Desk."
    ),
    terms = None
  )

  val onlineRequestAccessCondition = DisplayAccessCondition(
    method = CatalogueAccessMethod.OnlineRequest,
    status = CatalogueAccessStatus.Open
  )

  val availableDates = Some(
    List(
      AvailabilitySlot(
        "2024-04-25T09:00:00.000Z",
        "2024-04-25T19:00:00.000Z"
      ),
      AvailabilitySlot(
        "2024-04-26T09:00:00.000Z",
        "2024-04-26T17:00:00.000Z"
      )
    )
  )

  class DummyItemUpdater(
    itemTransform: Seq[DisplayItem] => Seq[DisplayItem] = identity
  ) extends ItemUpdater {
    override val identifierType: DisplayIdentifierType =
      DisplayIdentifierType.SierraSystemNumber

    override def updateItems(
      items: Seq[DisplayItem]
    ): Future[Seq[DisplayItem]] = Future {
      itemTransform(items)
    }
  }

  describe("with SierraItemUpdater") {
    val workWithUnavailableItemNumber = createSierraItemNumber
    val workWithAvailableItemNumber = createSierraItemNumber

    val workWithAvailableItem = CatalogueWork(
      id = createCanonicalId,
      title = None,
      identifiers = Nil,
      items = List(
        availableItem(workWithAvailableItemNumber)
      )
    )

    val itemStates = Table(
      (
        "Sierra Responses",
        "Content api Venue Responses",
        "Catalogue Work",
        "AccessCondition",
        "AvailableDates"
      ),
      (
        onHoldItemResponses(workWithAvailableItemNumber),
        Nil,
        List(availableItem(workWithAvailableItemNumber)),
        onHoldAccessCondition,
        None
      ),
      (
        onHoldItemResponses(workWithUnavailableItemNumber),
        Nil,
        List(
          temporarilyUnavailableItem(workWithUnavailableItemNumber)
        ),
        onHoldAccessCondition,
        None
      ),
      (
        availableItemResponses(workWithAvailableItemNumber),
        Seq((contentApiVenueRequest("library"), contentApiVenueResponse())),
        List(availableItem(workWithAvailableItemNumber)),
        onlineRequestAccessCondition,
        availableDates
      ),
      (
        availableItemResponses(workWithUnavailableItemNumber),
        Seq((contentApiVenueRequest("library"), contentApiVenueResponse())),
        List(
          temporarilyUnavailableItem(workWithUnavailableItemNumber)
        ),
        onlineRequestAccessCondition,
        availableDates
      ),
      (
        missingItemResponse(workWithAvailableItemNumber),
        Seq((contentApiVenueRequest("library"), contentApiVenueResponse())),
        List(availableItem(workWithAvailableItemNumber)),
        availableOnlineAccessCondition,
        availableDates
      )
    )

    it("updates AccessCondition correctly based on Sierra responses") {
      forAll(itemStates) {
        (
          sierraResponses,
          contentApiVenueResponse,
          items,
          expectedAccessCondition,
          _
        ) =>
          withClock("2018-04-29T10:15:30.00Z") { clock =>
            withSierraItemUpdater(
              sierraResponses,
              contentApiVenueResponse,
              clock
            ) { sierraItemUpdater =>
              whenReady(sierraItemUpdater.updateItems(items)) { updatedItems =>
                updatedItems.length shouldBe 1
                val physicalItem = updatedItems.head

                val updatedAccessCondition = physicalItem.locations.head
                  .asInstanceOf[DisplayPhysicalLocation]
                  .accessConditions
                  .head

                updatedAccessCondition shouldBe expectedAccessCondition
              }
            }
          }
      }
    }

    it(
      "adds correct available dates for on-site item, request made on working day before 10am"
    ) {
      withClock("2024-04-24T08:58:00.000Z") { clock =>
        withSierraItemUpdater(
          availableItemResponses(workWithAvailableItemNumber),
          Seq((contentApiVenueRequest("library"), contentApiVenueResponse())),
          clock
        ) { sierraItemUpdater =>
          whenReady(sierraItemUpdater.updateItems(workWithAvailableItem.items)) {
            updatedItems =>
              updatedItems.length shouldBe 1

              val physicalItem = updatedItems.head

              physicalItem.availableDates shouldBe Some(
                List(
                  AvailabilitySlot(
                    "2024-04-25T09:00:00.000Z",
                    "2024-04-25T19:00:00.000Z"
                  ),
                  AvailabilitySlot(
                    "2024-04-26T09:00:00.000Z",
                    "2024-04-26T17:00:00.000Z"
                  )
                )
              )
          }

        }
      }
    }

    it(
      "adds correct available dates for on-site item, request made on working day after 10am"
    ) {
      withClock() { clock =>
        withSierraItemUpdater(
          availableItemResponses(workWithAvailableItemNumber),
          Seq((contentApiVenueRequest("library"), contentApiVenueResponse())),
          clock
        ) { sierraItemUpdater =>
          whenReady(sierraItemUpdater.updateItems(workWithAvailableItem.items)) {
            updatedItems =>
              updatedItems.length shouldBe 1

              val physicalItem = updatedItems.head

              physicalItem.availableDates shouldBe Some(
                List(
                  AvailabilitySlot(
                    "2024-04-26T09:00:00.000Z",
                    "2024-04-26T17:00:00.000Z"
                  )
                )
              )
          }
        }
      }
    }

    it(
      "adds correct available dates for on-site item, request made on non-working day before 10am"
      // just to cover all cases, see below
    ) {
      withClock("2024-04-23T07:00:00.000Z") { clock =>
        withSierraItemUpdater(
          availableItemResponses(workWithAvailableItemNumber),
          Seq((contentApiVenueRequest("library"), contentApiVenueResponse())),
          clock
        ) { sierraItemUpdater =>
          whenReady(sierraItemUpdater.updateItems(workWithAvailableItem.items)) {
            updatedItems =>
              updatedItems.length shouldBe 1

              val physicalItem = updatedItems.head

              physicalItem.availableDates shouldBe Some(
                List(
                  AvailabilitySlot(
                    "2024-04-25T09:00:00.000Z",
                    "2024-04-25T19:00:00.000Z"
                  ),
                  AvailabilitySlot(
                    "2024-04-26T09:00:00.000Z",
                    "2024-04-26T17:00:00.000Z"
                  )
                )
              )
          }
        }
      }
    }

    it(
      "adds correct available dates for on-site item, request made on non-working day after 10am"
      // edge case: a request made after 10am on a closed day will reach the staff before 10am on the next open day
      // item will therefore available the following day
    ) {
      withClock("2024-04-23T13:00:00.000Z") { clock =>
        withSierraItemUpdater(
          availableItemResponses(workWithAvailableItemNumber),
          Seq((contentApiVenueRequest("library"), contentApiVenueResponse())),
          clock
        ) { sierraItemUpdater =>
          whenReady(sierraItemUpdater.updateItems(workWithAvailableItem.items)) {
            updatedItems =>
              updatedItems.length shouldBe 1
              val physicalItem = updatedItems.head

              physicalItem.availableDates shouldBe Some(
                List(
                  AvailabilitySlot(
                    "2024-04-25T09:00:00.000Z",
                    "2024-04-25T19:00:00.000Z"
                  ),
                  AvailabilitySlot(
                    "2024-04-26T09:00:00.000Z",
                    "2024-04-26T17:00:00.000Z"
                  )
                )
              )
          }
        }
      }
    }

    it(
      "adds correct available dates for deepstore item"
    ) {
      // a deepstore item requested on 23/04 will be available in the library 10 days later, from 04/05
      withClock("2024-04-23T13:00:00.000Z") { clock =>
        withSierraItemUpdater(
          availableDeepstoreItemResponse(workWithAvailableItemNumber),
          Seq(
            (
              contentApiVenueRequest("deepstore"),
              contentApiVenueResponse("deepstore")
            )
          ),
          clock
        ) { sierraItemUpdater =>
          whenReady(sierraItemUpdater.updateItems(workWithAvailableItem.items)) {
            updatedItems =>
              updatedItems.length shouldBe 1

              val physicalItem = updatedItems.head

              physicalItem.availableDates shouldBe Some(
                List(
                  AvailabilitySlot(
                    "2024-05-04T08:00:00.000Z",
                    "2024-05-04T15:00:00.000Z"
                  ),
                  AvailabilitySlot(
                    "2024-05-05T09:00:00.000Z",
                    "2024-05-05T17:00:00.000Z"
                  )
                )
              )
          }
        }
      }
    }

    it(
      "adds available dates as an empty list if contentApiVenueRequest returns an error"
    ) {
      withClock() { clock =>
        withSierraItemUpdater(
          availableItemResponses(workWithAvailableItemNumber),
          Seq(
            (
              contentApiVenueRequest("library"),
              contentApiVenueErrorResponse(500)
            )
          ),
          clock
        ) { sierraItemUpdater =>
          whenReady(sierraItemUpdater.updateItems(workWithAvailableItem.items)) {
            updatedItems =>
              updatedItems.length shouldBe 1

              val physicalItem = updatedItems.head

              physicalItem.availableDates shouldBe Some(
                List()
              )
          }
        }
      }
    }

    it(
      "tolerates missing locations and conditions"
    ) {
      val itemNumber = createSierraItemNumber
      val work = CatalogueWork(
        id = createCanonicalId,
        title = None,
        identifiers = Nil,
        items = List(
          DisplayItem(
            id = Some(createCanonicalId),
            identifiers = List(
              DisplayIdentifier(
                identifierType = DisplayIdentifierType.SierraSystemNumber,
                value = itemNumber.withCheckDigit
              )
            ),
            locations = Nil,
            availableDates = None
          )
        )
      )
      withClock() { clock =>
        withSierraItemUpdater(
          missingItemResponse(itemNumber),
          Seq(),
          clock
        ) { sierraItemUpdater =>
          whenReady(sierraItemUpdater.updateItems(work.items)) { updatedItems =>
            updatedItems.length shouldBe 1

            val physicalItem = updatedItems.head

            physicalItem.availableDates shouldBe None
          }
        }
      }
    }
  }

  def createPhysicalItemWith(
    sierraItemNumber: SierraItemNumber,
    accessCondition: DisplayAccessCondition
  ): DisplayItem = {

    val physicalItemLocation =
      DisplayPhysicalLocation(
        accessConditions = List(accessCondition),
        label = randomAlphanumeric(),
        locationType = DisplayLocationType(
          id = "closed-stores",
          label = "Closed stores"
        )
      )

    val itemSourceIdentifier = DisplayIdentifier(
      identifierType = DisplayIdentifierType.SierraSystemNumber,
      value = sierraItemNumber.withCheckDigit
    )

    DisplayItem(
      id = Some(createCanonicalId),
      identifiers = List(itemSourceIdentifier),
      locations = List(physicalItemLocation),
      availableDates = None
    )
  }
}
