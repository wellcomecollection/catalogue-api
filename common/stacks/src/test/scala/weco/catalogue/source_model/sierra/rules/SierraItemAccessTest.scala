package weco.catalogue.source_model.sierra.rules

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.stacks.models.CatalogueAccessMethod
import weco.catalogue.display_model.locations.{CatalogueAccessStatus, DisplayAccessCondition, DisplayLocationType}
import weco.sierra.generators.SierraDataGenerators
import weco.sierra.models.marc.FixedField

class SierraItemAccessTest
  extends AnyFunSpec
    with Matchers
    with SierraDataGenerators {

  val closedStores = DisplayLocationType(
    id = "closed-stores",
    label = "Closed stores"
  )

  val openShelves = DisplayLocationType(
    id = "open-shelves",
    label = "Open shelves"
  )

  describe("an item in the closed stores") {
    describe("with no holds") {
      describe("can be requested online") {
        it("if it has no restrictions") {
          val itemData = createSierraItemDataWith(
            fixedFields = Map(
              "79" -> FixedField(
                label = "LOCATION",
                value = "scmac",
                display = "Closed stores Arch. & MSS"),
              "88" -> FixedField(
                label = "STATUS",
                value = "-",
                display = "Available"),
              "108" -> FixedField(
                label = "OPACMSG",
                value = "f",
                display = "Online request"),
            )
          )

          val (ac, _) = SierraItemAccess(
            location = Some(closedStores),
            itemData = itemData
          )

          ac.get shouldBe DisplayAccessCondition(
            method = CatalogueAccessMethod.OnlineRequest,
            status = CatalogueAccessStatus.Open)
        }

        it("if it's restricted") {
          val itemData = createSierraItemDataWith(
            fixedFields = Map(
              "79" -> FixedField(
                label = "LOCATION",
                value = "scmac",
                display = "Closed stores Arch. & MSS"),
              "88" -> FixedField(
                label = "STATUS",
                value = "-",
                display = "Available"),
              "108" -> FixedField(
                label = "OPACMSG",
                value = "c",
                display = "Restricted"),
            )
          )

          val (ac, _) = SierraItemAccess(
            location = Some(closedStores),
            itemData = itemData
          )

          ac.get shouldBe
            DisplayAccessCondition(
              method = CatalogueAccessMethod.OnlineRequest,
              status = CatalogueAccessStatus.Restricted)
        }
      }

      describe("cannot be requested") {
        it("if it needs a manual request") {
          val itemData = createSierraItemDataWith(
            fixedFields = Map(
              "61" -> FixedField(
                label = "I TYPE",
                value = "4",
                display = "serial"),
              "79" -> FixedField(
                label = "LOCATION",
                value = "sgser",
                display = "Closed stores journals"),
              "88" -> FixedField(
                label = "STATUS",
                value = "-",
                display = "Available"),
              "108" -> FixedField(
                label = "OPACMSG",
                value = "n",
                display = "Manual request"),
            )
          )

          val (ac, _) = SierraItemAccess(
            location = Some(closedStores),
            itemData = itemData
          )

          ac shouldBe None
        }
      }
    }

    describe("that's on hold can't be requested") {
      it("when another reader has placed a hold") {
        val itemData = createSierraItemDataWith(
          holdCount = Some(1),
          fixedFields = Map(
            "79" -> FixedField(
              label = "LOCATION",
              value = "sgeph",
              display = "Closed stores ephemera"),
            "88" -> FixedField(
              label = "STATUS",
              value = "-",
              display = "Available"),
            "108" -> FixedField(
              label = "OPACMSG",
              value = "f",
              display = "Online request"),
          )
        )

        val (ac, _) = SierraItemAccess(
          location = Some(closedStores),
          itemData = itemData
        )

        ac.get shouldBe
          DisplayAccessCondition(
            method = CatalogueAccessMethod.NotRequestable,
            status = Some(CatalogueAccessStatus.TemporarilyUnavailable),
            note = Some(
              "Item is in use by another reader. Please ask at Library Enquiry Desk."),
            terms = None
          )
      }

      it("when an item is on hold for a loan rule") {
        val itemData = createSierraItemDataWith(
          holdCount = Some(1),
          fixedFields = Map(
            "79" -> FixedField(
              label = "LOCATION",
              value = "sgeph",
              display = "Closed stores ephemera"),
            "87" -> FixedField(label = "LOANRULE", value = "5"),
            "88" -> FixedField(
              label = "STATUS",
              value = "-",
              display = "Available"),
            "108" -> FixedField(
              label = "OPACMSG",
              value = "f",
              display = "Online request"),
          )
        )

        val (ac, _) = SierraItemAccess(
          location = Some(closedStores),
          itemData = itemData
        )

        ac.get shouldBe
          DisplayAccessCondition(
            method = CatalogueAccessMethod.NotRequestable,
            status = Some(CatalogueAccessStatus.TemporarilyUnavailable),
            note = Some(
              "Item is in use by another reader. Please ask at Library Enquiry Desk."),
            terms = None
          )
      }

      it("when a manual request item is on hold for somebody else") {
        val itemData = createSierraItemDataWith(
          holdCount = Some(1),
          fixedFields = Map(
            "61" -> FixedField(
              label = "I TYPE",
              value = "4",
              display = "serial"),
            "79" -> FixedField(
              label = "LOCATION",
              value = "sgser",
              display = "Closed stores journals"),
            "88" -> FixedField(
              label = "STATUS",
              value = "-",
              display = "Available"),
            "108" -> FixedField(
              label = "OPACMSG",
              value = "n",
              display = "Manual request"),
          )
        )

        val (ac, _) = SierraItemAccess(
          location = Some(closedStores),
          itemData = itemData
        )

        ac.get shouldBe
          DisplayAccessCondition(
            method = CatalogueAccessMethod.NotRequestable,
            status = Some(CatalogueAccessStatus.TemporarilyUnavailable),
            note = Some(
              "Item is in use by another reader. Please ask at Library Enquiry Desk."),
            terms = None
          )
      }

      it("when it's on the hold shelf for another reader") {
        val itemData = createSierraItemDataWith(
          holdCount = Some(1),
          fixedFields = Map(
            "79" -> FixedField(
              label = "LOCATION",
              value = "swms4",
              display = "Closed stores WMS 4"),
            "88" -> FixedField(
              label = "STATUS",
              value = "!",
              display = "On holdshelf"),
            "108" -> FixedField(
              label = "OPACMSG",
              value = "f",
              display = "Online request"),
          )
        )

        val (ac, _) = SierraItemAccess(
          location = Some(closedStores),
          itemData = itemData
        )

        ac.get shouldBe
          DisplayAccessCondition(
            method = CatalogueAccessMethod.NotRequestable,
            status = Some(CatalogueAccessStatus.TemporarilyUnavailable),
            note = Some(
              "Item is in use by another reader. Please ask at Library Enquiry Desk."),
            terms = None
          )
      }
    }
  }

  describe("an item on the open shelves") {
    describe("with no holds or other restrictions") {
      it("cannot be requested online") {
        val itemData = createSierraItemDataWith(
          fixedFields = Map(
            "79" -> FixedField(
              label = "LOCATION",
              value = "wgmem",
              display = "Medical Collection"),
            "88" -> FixedField(
              label = "STATUS",
              value = "-",
              display = "Available"),
            "108" -> FixedField(
              label = "OPACMSG",
              value = "o",
              display = "Open shelves"),
          )
        )

        val (ac, _) = SierraItemAccess(
          location = Some(openShelves),
          itemData = itemData
        )

        ac shouldBe None
      }
    }
  }
}
