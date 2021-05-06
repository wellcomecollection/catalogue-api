package uk.ac.wellcome.platform.api.common.services

import java.time.Instant

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.platform.api.common.fixtures.ServicesFixture
import uk.ac.wellcome.platform.api.common.models._
import weco.catalogue.internal_model.identifiers.CanonicalId

class StacksServiceTest
    extends AnyFunSpec
    with ServicesFixture
    with ScalaFutures
    with IntegrationPatience
    with Matchers {

  describe("StacksService") {
    describe("getStacksUserHoldsWithStacksItemIdentifier") {
      it("gets a StacksUserHolds[StacksItemIdentifier]") {
        withStacksService {
          case (stacksService, _, _) =>
            val stacksUserIdentifier = StacksUserIdentifier("1234567")

            whenReady(
              stacksService.getStacksUserHolds(
                userId = stacksUserIdentifier
              )
            ) { stacksUserHolds =>
              stacksUserHolds shouldBe StacksUserHolds(
                userId = "1234567",
                holds = List(
                  StacksHold(
                    itemId = StacksItemIdentifier(
                      canonicalId = CanonicalId("n5v7b4md"),
                      sierraId = SierraItemIdentifier(1292185)
                    ),
                    pickup = StacksPickup(
                      location =
                        StacksPickupLocation("sepbb", "Rare Materials Room"),
                      pickUpBy = Some(Instant.parse("2019-12-03T04:00:00Z"))
                    ),
                    status =
                      StacksHoldStatus("i", "item hold ready for pickup.")
                  )
                )
              )
            }
        }
      }
    }
  }
}
