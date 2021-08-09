package weco.api.items.fixtures

import akka.http.scaladsl.model.Uri
import weco.api.stacks.fixtures.SierraServiceFixture
import weco.catalogue.source_model.sierra.identifiers.SierraItemNumber

trait SierraItemUriFixture extends SierraServiceFixture {
  def sierraUri(sierraItemNumber: SierraItemNumber): Uri =
    Uri(
      f"http://sierra:1234/v5/items?id=${sierraItemNumber.withoutCheckDigit}&fields=deleted,fixedFields,holdCount,suppressed"
    )
}
