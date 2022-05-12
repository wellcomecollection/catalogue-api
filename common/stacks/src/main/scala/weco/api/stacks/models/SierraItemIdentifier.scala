package weco.api.stacks.models

import weco.sierra.models.identifiers.SierraItemNumber

import java.net.URI
import scala.util.{Failure, Success, Try}

object SierraItemIdentifier {
  def fromUrl(url: URI): SierraItemNumber =
    Try {
      // This value looks like a URI when provided by Sierra
      // e.g. https://libsys.wellcomelibrary.org/iii/sierra-api/v5/items/1292185
      url.getPath.split("/").last
    } match {
      case Success(id) => SierraItemNumber(id)
      case Failure(e) =>
        throw new Exception("Failed to create SierraItemNumber", e)
    }

  def toCatalogueIdentifier(itemNumber: SierraItemNumber): CatalogueIdentifier =
    CatalogueIdentifier(
      identifierType = CatalogueIdentifierType("sierra-system-number"),
      value = itemNumber.withCheckDigit
    )

  def fromSourceIdentifier(
    sourceIdentifier: CatalogueIdentifier
  ): SierraItemNumber = {
    require(
      sourceIdentifier.identifierType == CatalogueIdentifierType(
        "sierra-system-number"
      )
    )

    // We expect the SourceIdentifier to have a Sierra ID with a prefix
    // and a check digit, e.g. i18234495
    require(sourceIdentifier.value.length == 9)
    val itemNumber = SierraItemNumber(sourceIdentifier.value.slice(1, 8))

    require(itemNumber.withCheckDigit == sourceIdentifier.value)
    itemNumber
  }
}
