package uk.ac.wellcome.platform.api.common.models

import weco.api.stacks.models.SierraItemNumber
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraSystemNumber
import weco.catalogue.internal_model.identifiers.{CanonicalId, SourceIdentifier}

import scala.util.{Failure, Success, Try}

sealed trait Identifier[T] {
  val value: T

  override def toString: String = value.toString
}

case class StacksUserIdentifier(value: String) extends Identifier[String]

sealed trait ItemIdentifier[T] extends Identifier[T]

case class SierraItemIdentifier(value: Long) extends ItemIdentifier[Long]

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

case class StacksItemIdentifier(
  canonicalId: CanonicalId,
  sierraId: SierraItemIdentifier
) extends ItemIdentifier[String] {
  override val value: String = canonicalId.toString

  override def toString: String =
    s"<StacksItemIdentifier catalogue=$canonicalId, sierra=$sierraId>"
}
