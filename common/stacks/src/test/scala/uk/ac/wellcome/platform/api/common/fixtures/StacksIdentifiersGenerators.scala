package uk.ac.wellcome.platform.api.common.fixtures

import uk.ac.wellcome.platform.api.common.models.{
  SierraItemIdentifier,
  StacksItemIdentifier
}

import weco.catalogue.internal_model.generators.IdentifiersGenerators

import scala.util.Random

trait StacksIdentifiersGenerators extends IdentifiersGenerators {

  def maybe[T](t: T): Option[T] =
    if (Random.nextBoolean()) Some(t) else None

  // Sierra identifiers are 7-digit numbers
  def createSierraItemIdentifier: SierraItemIdentifier =
    SierraItemIdentifier(
      (Random.nextFloat * 1000000).toLong
    )

  def createStacksItemIdentifier: StacksItemIdentifier =
    StacksItemIdentifier(
      canonicalId = createCanonicalId,
      sierraId = createSierraItemIdentifier
    )
}
