package weco.api.stacks.http.impl

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.platform.api.common.fixtures.SierraWireMockFixture
import weco.api.stacks.http.SierraAuthenticatedHttpClient
import weco.http.fixtures.HttpFixtures

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global

class AkkaHttpClientTest extends AnyFunSpec with Matchers with ScalaFutures with IntegrationPatience with Akka with SierraWireMockFixture with HttpFixtures {
  it("requests an item, fetching a token first") {
    withMockSierraServer {
      case (sierraApiUrl, _) =>
        withActorSystem { implicit actorSystem =>
          val httpClient = new AkkaHttpClient(
            baseUri = Uri(s"$sierraApiUrl/iii/sierra-api")
          )
          val client = new SierraAuthenticatedHttpClient(
            underlying = httpClient,
            credentials = BasicHttpCredentials("username", "password")
          )

          val future = client.get(
            path = Path(f"v5/items/1601017")
          )

          whenReady(future) { resp =>
            resp.status shouldBe StatusCodes.OK

            withStringEntity(resp.entity) {
              assertJsonStringsAreEqual(_,
                s"""
                   |{
                   |  "id": "1601017",
                   |  "updatedDate": "2009-06-15T14:48:00Z",
                   |  "createdDate": "2008-05-21T12:47:00Z",
                   |  "deleted": false,
                   |  "bibIds": [
                   |    "1665618"
                   |  ],
                   |  "location": {
                   |    "code": "sicon",
                   |    "name": "Closed stores Iconographic"
                   |  },
                   |  "status": {
                   |    "code": "-  ",
                   |    "display": "Available"
                   |  },
                   |  "barcode": "D59.14 (Germany file)",
                   |  "callNumber": "665618i"
                   |}
                   |""".stripMargin)
            }
          }
        }
    }
  }

  override def contextUrl: URL = new URL("http://test.example/context.json")
}
