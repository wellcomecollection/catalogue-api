package weco.api.stacks.models

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.catalogue.display_model.locations.{DisplayAccessCondition, DisplayAccessMethod, DisplayAccessStatus}

class DisplayItemOpsTest extends AnyFunSpec with Matchers with DisplayItemOps {
  it("marks an access condition as stale if it's requestable") {
    val ac = DisplayAccessCondition(
      method = DisplayAccessMethod(id = "online-request", label = "Online request")
    )

    ac.isStale shouldBe true
  }

  it("marks an access condition as stale if it's temporarily unavailable") {
    val ac = DisplayAccessCondition(
      method = DisplayAccessMethod(id = "not-requestable", label = "Not requestable"),
      status = DisplayAccessStatus(id = "temporarily-unavailable", label = "Temporarily unavailable")
    )

    ac.isStale shouldBe true
  }

  it("doesn't mark an access condition as stale if it's unavailable") {
    val ac = DisplayAccessCondition(
      method = DisplayAccessMethod(id = "not-requestable", label = "Not requestable"),
      status = DisplayAccessStatus(id = "unavailable", label = "Unavailable")
    )

    ac.isStale shouldBe false
  }

  it("doesn't mark an access condition as stale if it's temporarily unavailable for conservation") {
    val ac = DisplayAccessCondition(
      method = DisplayAccessMethod(id = "not-requestable", label = "Not requestable"),
      status = Some(
        DisplayAccessStatus(id = "temporarily-unavailable", label = "Temporarily unavailable")
      ),
      note = Some(
        "This item is being digitised and is currently unavailable."
      ),
      terms = None
    )

    ac.isStale shouldBe false
  }
}
