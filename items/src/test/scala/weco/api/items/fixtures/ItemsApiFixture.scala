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
import weco.api.items.services.LondonClock

import scala.concurrent.ExecutionContext.Implicits.global
import java.time.{ZoneId, ZonedDateTime}

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

  def withItemsApi[R](
    catalogueResponses: Seq[(HttpRequest, HttpResponse)] = Seq(),
    sierraResponses: Seq[(HttpRequest, HttpResponse)] = Seq(),
    contentApiVenueResponses: Seq[(HttpRequest, HttpResponse)] = Seq(),
    time: Int
  )(testWith: TestWith[Unit, R]): R =
    withActorSystem { implicit actorSystem =>
      withSierraSource(sierraResponses) { sierraSource =>
        val contentApiClient = new MemoryHttpClient(contentApiVenueResponses)
        with HttpGet {
          override val baseUri: Uri = Uri("http://content:9002")
        }
        val mockTime = ZonedDateTime
          .of(2024, 4, 24, time, 0, 0, 0, ZoneId.of("Europe/London"))
        val londonClock = new LondonClock {
          override def timeInLondon(): ZonedDateTime = mockTime
        }

        val itemsUpdaters = List(
          new SierraItemUpdater(
            sierraSource,
            new VenueOpeningTimesLookup(contentApiClient),
            londonClock
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
