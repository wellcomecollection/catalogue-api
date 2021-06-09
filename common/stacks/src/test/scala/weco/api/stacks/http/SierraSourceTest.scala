package weco.api.stacks.http

import akka.http.scaladsl.model._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.platform.api.common.services.source.SierraSource.{SierraItemStatusStub, SierraItemStub}
import weco.api.stacks.http.impl.MemoryHttpClient
import weco.catalogue.source_model.sierra.identifiers.SierraItemNumber

import scala.concurrent.ExecutionContext.Implicits.global

class SierraSourceTest extends AnyFunSpec with Matchers with Akka with ScalaFutures with IntegrationPatience {
  def withSource[R](responses: Seq[(HttpRequest, HttpResponse)])(testWith: TestWith[SierraSource, R]): R =
    withMaterializer { implicit mat =>
      val client = new MemoryHttpClient(responses = responses)

      val source = new SierraSource(client)

      testWith(source)
    }

  describe("lookupItem") {
    it("looks up a single item") {
      val itemNumber = SierraItemNumber("1146055")

      val responses = Seq(
        (
          HttpRequest(
            uri = Uri("http://sierra:1234/v5/items/1146055")
          ),
          HttpResponse(
            entity = HttpEntity(
              contentType = ContentTypes.`application/json`,
              """
                |{
                |  "id": "1146055",
                |  "updatedDate": "2021-06-09T13:23:27Z",
                |  "createdDate": "1999-11-15T18:56:00Z",
                |  "deleted": false,
                |  "bibIds": [
                |    "1126829"
                |  ],
                |  "location": {
                |    "code": "sgmed",
                |    "name": "Closed stores Med."
                |  },
                |  "status": {
                |    "code": "t",
                |    "display": "In quarantine"
                |  },
                |  "volumes": [],
                |  "barcode": "22500271327",
                |  "callNumber": "K33043"
                |}
                |
                |""".stripMargin
            )
          )
        )
      )

      withSource(responses) { source =>
        val future = source.lookupItem(itemNumber)

        whenReady(future) {
          _ shouldBe Right(
            SierraItemStub(
              id = itemNumber,
              status = SierraItemStatusStub(
                code = "t",
                display = "In quarantine"
              )
            )
          )
        }
      }
    }
  }
}
