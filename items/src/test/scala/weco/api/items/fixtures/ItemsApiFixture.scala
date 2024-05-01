package weco.api.items.fixtures

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import org.scalatest.Suite
import weco.api.items.ItemsApi
import weco.api.items.services.{
  ItemUpdateService,
  SierraItemUpdater,
  VenueOpeningTimesLookup,
  WorkLookup
}
import weco.api.search.models.ApiConfig
import weco.fixtures.TestWith
import weco.http.client.{HttpGet, MemoryHttpClient}
import weco.sierra.fixtures.SierraSourceFixture

import scala.concurrent.ExecutionContext.Implicits.global
import java.time.{Clock, Instant, ZoneId}

trait ItemsApiFixture extends SierraSourceFixture {
  this: Suite =>

  val metricsName = "ItemsApiFixture"

  implicit val apiConfig: ApiConfig =
    ApiConfig(
      publicHost = "localhost",
      publicScheme = "https",
      defaultPageSize = 10,
      publicRootPath = "/catalogue"
    )

  def withClock[R](
    dateTime: String = "2024-04-24T11:00:00.000Z"
  )(testWith: TestWith[Clock, R]): R = {
    val instant = Instant.parse(dateTime)
    val zoneId = ZoneId.of("Europe/London")
    testWith(Clock.fixed(instant, zoneId))
  }

  def withItemsApi[R](
    catalogueResponses: Seq[(HttpRequest, HttpResponse)] = Seq(),
    sierraResponses: Seq[(HttpRequest, HttpResponse)] = Seq(),
    contentApiVenueResponses: Seq[(HttpRequest, HttpResponse)] = Seq()
  )(testWith: TestWith[Unit, R]): R =
    withActorSystem { implicit actorSystem =>
      withClock() { clock =>
        withSierraSource(sierraResponses) { sierraSource =>
          val contentApiClient = new MemoryHttpClient(contentApiVenueResponses)
          with HttpGet {
            override val baseUri: Uri = Uri("http://content:9002")
          }

          val itemsUpdaters = List(
            new SierraItemUpdater(
              sierraSource,
              new VenueOpeningTimesLookup(contentApiClient),
              clock
            )
          )

          val catalogueApiClient = new MemoryHttpClient(catalogueResponses)
          with HttpGet {
            override val baseUri: Uri = Uri("http://catalogue:9001")
          }

          val api: ItemsApi = new ItemsApi(
            itemUpdateService = new ItemUpdateService(itemsUpdaters),
            workLookup = new WorkLookup(catalogueApiClient)
          )

          withApp(api.routes) { _ =>
            testWith(())
          }
        }
      }
    }
}
