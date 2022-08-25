package weco.catalogue.source_model.sierra.rules

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import weco.sierra.generators.SierraDataGenerators
import weco.sierra.models.marc.FixedField

class SierraRulesForRequestingTest
  extends AnyFunSpec
    with Matchers
    with SierraDataGenerators
    with TableDrivenPropertyChecks {
  it("blocks an item based on the status") {
    val testCases = Table(
      "status",
      "m",
      "s",
      "x",
      "r",
      "z",
      "v",
      "h",
      "b",
      "c",
      "d",
      "e",
      "y",
    )

    forAll(testCases) {
      status =>
        val item = createSierraItemDataWith(
          fixedFields =
            Map("88" -> FixedField(label = "STATUS", value = status))
        )

        SierraRulesForRequesting(item) shouldBe None
    }
  }

  it("blocks an item if fixed field 87 (loan rule) is non-zero") {
    val item = createSierraItemDataWith(
      fixedFields = Map("87" -> FixedField(label = "LOANRULE", value = "1"))
    )

    SierraRulesForRequesting(item) shouldBe Some(
      NotRequestable.InUseByAnotherReader(
        "Item is in use by another reader. Please ask at Enquiry Desk.")
    )
  }

  it("blocks an item if fixed field 88 (status) is !") {
    val item = createSierraItemDataWith(
      fixedFields = Map("88" -> FixedField(label = "STATUS", value = "!"))
    )

    SierraRulesForRequesting(item) shouldBe Some(
      NotRequestable.InUseByAnotherReader(
        "Item is in use by another reader. Please ask at Enquiry Desk.")
    )
  }

  it("does not block an item if fixed field 87 (loan rule) is zero") {
    val item = createSierraItemDataWith(
      fixedFields = Map("87" -> FixedField(label = "LOANRULE", value = "0"))
    )

    SierraRulesForRequesting(item) shouldBe None
  }

  it("allows an item that does not match any rules") {
    val testCases = Table(
      "item",
      createSierraItemData,
      createSierraItemDataWith(
        fixedFields = Map(
          "79" -> FixedField(label = "LOCATION", value = "sicon")
        )
      ),
      createSierraItemDataWith(
        fixedFields = Map(
          "87" -> FixedField(label = "LOANRULE", value = "0")
        )
      ),
      createSierraItemDataWith(
        fixedFields = Map(
          "61" -> FixedField(label = "I TYPE", value = "5")
        )
      ),
    )

    forAll(testCases) {
      SierraRulesForRequesting(_) shouldBe None
    }
  }
}
