package weco.api.stacks.models

import weco.catalogue.source_model.sierra.identifiers.SierraItemNumber

case class SierraHoldRequest(
  recordType: String,
  recordNumber: Long,
  pickupLocation: String
)

case object SierraHoldRequest {
  def apply(item: SierraItemNumber): SierraHoldRequest =
    SierraHoldRequest(
      recordType = "i",
      recordNumber = item.withoutCheckDigit.toLong,
      // This field is required non-empty by the Sierra API - but has no effect
      // TODO: Is it really?
      pickupLocation = "unspecified"
    )
}
