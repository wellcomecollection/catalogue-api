package weco.catalogue.display_model.generators

import weco.fixtures.RandomGenerators

trait IdentifiersGenerators extends RandomGenerators {
  def createCanonicalId: String =
    randomAlphanumeric(length = 8).toLowerCase
}
