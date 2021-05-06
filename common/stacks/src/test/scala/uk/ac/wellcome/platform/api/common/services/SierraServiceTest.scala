package uk.ac.wellcome.platform.api.common.services

import java.time.Instant
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import uk.ac.wellcome.platform.api.common.fixtures.ServicesFixture
import uk.ac.wellcome.platform.api.common.models._
import com.github.tomakehurst.wiremock.client.WireMock._
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraSystemNumber
import weco.catalogue.internal_model.identifiers.SourceIdentifier

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
            val stacksUserIdentifier = StacksUserIdentifier("1234567")

            whenReady(
              sierraService.getStacksUserHolds(stacksUserIdentifier)
            ) { stacksUserHolds =>
              stacksUserHolds shouldBe StacksUserHolds(
                userId = "1234567",
                holds = List(
                  StacksHold(
                    itemId = SierraItemIdentifier(1292185),
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
            val sierraItemIdentifier = SierraItemIdentifier(1601017)
            val stacksUserIdentifier = StacksUserIdentifier("1234567")
            val neededBy = Some(
              Instant.parse("2020-01-01T00:00:00.00Z")
            )

            whenReady(
              sierraService.placeHold(
                userIdentifier = stacksUserIdentifier,
                sierraItemIdentifier = sierraItemIdentifier,
                neededBy = neededBy
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
                |  "pickupLocation" : "unspecified",
                |  "neededBy" : "2020-01-01"
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
            val sierraItemIdentifier = SierraItemIdentifier(1601018)
            val stacksUserIdentifier = StacksUserIdentifier("1234567")

            whenReady(
              sierraService.placeHold(
                userIdentifier = stacksUserIdentifier,
                sierraItemIdentifier = sierraItemIdentifier,
                neededBy = None
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
