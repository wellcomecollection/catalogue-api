package weco.api.stacks.models

import weco.catalogue.internal_model.identifiers.IdentifierType.SierraSystemNumber
import weco.catalogue.internal_model.identifiers.SourceIdentifier

import scala.util.{Failure, Success, Try}

object SierraItemIdentifier {
  def createFromSierraId(id: String): SourceIdentifier =
    Try {
      // This value looks like a URI when provided by Sierra
      // https://libsys.wellcomelibrary.org/iii/sierra-api/v5/patrons/holds/145730
      id.split("/").last
    } match {
      case Success(v) =>
        SourceIdentifier(
          identifierType = SierraSystemNumber,
          ontologyType = "Item",
          value = SierraItemNumber(v).withCheckDigit
        )
      case Failure(e) =>
        throw new Exception("Failed to create SierraItemIdentifier", e)
    }
}
