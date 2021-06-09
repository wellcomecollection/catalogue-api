package uk.ac.wellcome.platform.api.common.services

import java.time.Instant
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import uk.ac.wellcome.platform.api.common.fixtures.ServicesFixture
import uk.ac.wellcome.platform.api.common.models._
import com.github.tomakehurst.wiremock.client.WireMock._
import weco.api.stacks.models.HoldRejected
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraSystemNumber
import weco.catalogue.internal_model.identifiers.SourceIdentifier
import weco.catalogue.source_model.sierra.identifiers.SierraPatronNumber

class SierraServiceTest
    extends AnyFunSpec
    with ServicesFixture
    with ScalaFutures
    with IntegrationPatience
    with EitherValues
    with Matchers {

  describe("SierraService") {
    describe("getItemStatus") {
      it("gets a StacksItemStatus") {
        withSierraService {
          case (sierraService, _) =>
            val identifier = SourceIdentifier(
              identifierType = SierraSystemNumber,
              value = "i16010176",
              ontologyType = "Item"
            )

            val future = sierraService.getItemStatus(identifier)

            whenReady(future) {
              _ shouldBe StacksItemStatus("available", "Available")
            }
        }
      }
    }

    describe("getStacksUserHolds") {
      it("gets a StacksUserHolds") {
        withSierraService {
          case (sierraService, _) =>
            val patronNumber = SierraPatronNumber("1234567")

            whenReady(
              sierraService.getStacksUserHolds(patronNumber)
            ) { stacksUserHolds =>
              stacksUserHolds shouldBe StacksUserHolds(
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
                patronNumber = patronNumber,
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
        withSierraService {
          case (sierraService, wireMockServer) =>
            val patronNumber = SierraPatronNumber("1234567")
            val sourceIdentifier = SourceIdentifier(
              identifierType = SierraSystemNumber,
              ontologyType = "Item",
              value = "i16010188"
            )

            whenReady(
              sierraService.placeHold(
                patronNumber = patronNumber,
                sourceIdentifier = sourceIdentifier,
              )
            ) { response =>
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
                                |  "recordNumber" : 1601018,
                                |  "pickupLocation" : "unspecified"
                                |}
                                |""".stripMargin)
                )
              )

              response shouldBe a[HoldRejected]
            }
        }
      }
    }
  }
}
