package weco.catalogue.display_model.generators

import weco.fixtures.RandomGenerators

trait IdentifiersGenerators extends RandomGenerators {

  /** Create something that looks plausibly like a canonical ID.
    *
    * The APIs have loose heuristics to filter out values that obviously
    * aren't canonical IDs (e.g. JavaScript injection attacks); one of those
    * includes checking the canonical ID is lowercase.
    *
    * Tests should prefer to use this helper over calling randomAlphanumeric()
    * directly.
    */
  def createCanonicalId: String =
    randomAlphanumeric(length = 8).toLowerCase
}
