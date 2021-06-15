package uk.ac.wellcome.platform.api.common.services

import akka.http.scaladsl.model._
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.platform.api.common.fixtures.ServicesFixture
import uk.ac.wellcome.platform.api.common.models._
import weco.api.stacks.models.CannotBeRequested
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraSystemNumber
import weco.catalogue.internal_model.identifiers.SourceIdentifier
import weco.catalogue.source_model.generators.SierraGenerators
import weco.catalogue.source_model.sierra.identifiers.SierraPatronNumber
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class SierraServiceTest
    extends AnyFunSpec
    with ServicesFixture
    with ScalaFutures
    with IntegrationPatience
    with EitherValues
    with Matchers
    with SierraGenerators {

  describe("SierraService") {
    describe("getItemStatus") {
      it("gets a StacksItemStatus") {
        val responses = Seq(
          (
            HttpRequest(uri =
              "http://sierra:1234/v5/items/1601017?fields=deleted,status,suppressed"),
            HttpResponse(
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                """
                   |{
                   |  "id": "1601017",
                   |  "deleted": false,
                   |  "suppressed": false,
                   |  "status": {"code": "-", "display": "Available"}
                   |}
                   |""".stripMargin
              )
            )
          )
        )

        withMaterializer { implicit mat =>
          val service = SierraService(
            client = new MemoryHttpClient(responses) with HttpGet
            with HttpPost {
              override val baseUri: Uri = Uri("http://sierra:1234")
            }
          )

          val identifier = SourceIdentifier(
            identifierType = SierraSystemNumber,
            value = "i16010176",
            ontologyType = "Item"
          )

          val future = service.getItemStatus(identifier)

          whenReady(future) {
            _.value shouldBe StacksItemStatus("available", "Available")
          }
        }
      }
    }

    describe("getStacksUserHolds") {
      it("gets a StacksUserHolds") {
        withSierraService {
          case (sierraService, _) =>
            val patronNumber = SierraPatronNumber("1234567")

            val future = sierraService.getStacksUserHolds(patronNumber)

            whenReady(future) { stacksUserHolds =>
              stacksUserHolds.value shouldBe StacksUserHolds(
                userId = "1234567",
                holds = List(
                  StacksHold(
                    sourceIdentifier = SourceIdentifier(
                      ontologyType = "Item",
                      identifierType = SierraSystemNumber,
                      value = "i12921853"
                    ),
                    pickup = StacksPickup(
                      location = StacksPickupLocation(
                        id = "sepbb",
                        label = "Rare Materials Room"
                      ),
                      pickUpBy = Some(Instant.parse("2019-12-03T04:00:00Z"))
                    ),
                    status = StacksHoldStatus(
                      id = "i",
                      label = "item hold ready for pickup."
                    )
                  )
                )
              )
            }
        }
      }
    }

    describe("placeHold") {
      it("requests a hold from the Sierra API") {
        withSierraService {
          case (sierraService, wireMockServer) =>
            val patronNumber = SierraPatronNumber("1234567")
            val sourceIdentifier = SourceIdentifier(
              identifierType = SierraSystemNumber,
              ontologyType = "Item",
              value = "i16010176"
            )

            whenReady(
              sierraService.placeHold(
                patron = patronNumber,
                sourceIdentifier = sourceIdentifier
              )
            ) { _ =>
              wireMockServer.verify(
                1,
                postRequestedFor(
                  urlEqualTo(
                    "/iii/sierra-api/v5/patrons/1234567/holds/requests"
                  )
                ).withRequestBody(
                  equalToJson("""
                |{
                |  "recordType" : "i",
                |  "recordNumber" : 1601017,
                |  "pickupLocation" : "unspecified"
                |}
                |""".stripMargin)
                )
              )
            }
        }
      }

      it("rejects a hold when the Sierra API errors indicating such") {
        val patron = SierraPatronNumber("1234567")
        val item = createSierraItemNumber
        val sourceIdentifier = SourceIdentifier(
          identifierType = SierraSystemNumber,
          ontologyType = "Item",
          value = item.withCheckDigit
        )

        val responses = Seq(
          (
            HttpRequest(
              method = HttpMethods.POST,
              uri = s"http://sierra:1234/v5/patrons/$patron/holds/requests",
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                s"""
                   |{
                   |  "recordType": "i",
                   |  "recordNumber": ${item.withoutCheckDigit},
                   |  "pickupLocation": "unspecified"
                   |}
                   |""".stripMargin
              )
            ),
            HttpResponse(
              status = StatusCodes.InternalServerError,
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                """
                  |{
                  |  "code": 132,
                  |  "specificCode": 2,
                  |  "httpStatus": 500,
                  |  "name": "XCirc error",
                  |  "description": "XCirc error : This record is not available"
                  |}
                  |""".stripMargin
              )
            )
          ),
          (
            HttpRequest(uri =
              s"http://sierra:1234/v5/patrons/$patron/holds?limit=100&offset=0"),
            HttpResponse(
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                s"""
                   |{
                   |  "total": 0,
                   |  "start": 0,
                   |  "entries": []
                   |}
                   |""".stripMargin
              )
            )
          ),
          (
            HttpRequest(uri =
              s"http://sierra:1234/v5/items/$item?fields=deleted,status,suppressed"),
            HttpResponse(
              entity = HttpEntity(
                contentType = ContentTypes.`application/json`,
                s"""
                   |{
                   |  "id": "$item",
                   |  "deletedDate": "2001-01-01",
                   |  "deleted": false,
                   |  "suppressed": true,
                   |  "status": {"code": "-", "display": "Available"}
                   |}
                   |""".stripMargin
              )
            )
          )
        )

        withMaterializer { implicit mat =>
          val service = SierraService(
            new MemoryHttpClient(responses) with HttpGet with HttpPost {
              override val baseUri: Uri = Uri("http://sierra:1234")
            }
          )

          val future = service.placeHold(patron = patron, sourceIdentifier = sourceIdentifier)

          whenReady(future) {
            _ shouldBe a[CannotBeRequested]
          }
        }
      }
    }
  }
}
