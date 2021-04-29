package uk.ac.wellcome.platform.api.common.models

import weco.catalogue.internal_model.identifiers.CanonicalId

import scala.util.{Failure, Success, Try}

sealed trait Identifier[T] {
  val value: T

  override def toString: String = value.toString
}

case class StacksUserIdentifier(value: String) extends Identifier[String]

sealed trait ItemIdentifier[T] extends Identifier[T]

case class SierraItemIdentifier(value: Long) extends ItemIdentifier[Long]

object SierraItemIdentifier {
  def createFromSierraId(id: String): SierraItemIdentifier =
    Try {
      // This value looks like a URI when provided by Sierra
      // https://libsys.wellcomelibrary.org/iii/sierra-api/v5/patrons/holds/145730
      id.split("/").last.toLong
    } match {
      case Success(v) => SierraItemIdentifier(v)
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
