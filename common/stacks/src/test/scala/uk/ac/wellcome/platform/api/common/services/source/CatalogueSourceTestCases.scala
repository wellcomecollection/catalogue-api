package uk.ac.wellcome.platform.api.common.services.source

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.platform.api.common.models.{SierraItemIdentifier}
import uk.ac.wellcome.platform.api.common.services.source.CatalogueSource._
import weco.catalogue.internal_model.generators.IdentifiersGenerators
import weco.catalogue.internal_model.identifiers.CanonicalId

trait CatalogueSourceTestCases[CatalogueSourceImpl <: CatalogueSource]
    extends AnyFunSpec
    with Matchers
    with IdentifiersGenerators
    with ScalaFutures {
  def withCatalogueSource[R](testWith: TestWith[CatalogueSourceImpl, R]): R

  describe("behaves as a CatalogueSource") {
    it("gets an individual work") {
      withCatalogueSource { catalogueSource =>
        val canonicalId = CanonicalId("ayzrznsz")

        val future =
          catalogueSource.getWorkStub(id = canonicalId)

        val expectedWork = WorkStub(
          id = canonicalId.toString(),
          items = List(
            ItemStub(
              id = Some("q2knsrhh"),
              identifiers = Some(
                List(
                  IdentifiersStub(
                    identifierType = TypeStub(
                      id = "sierra-system-number",
                      label = "Sierra system number"
                    ),
                    value = "i10938370"
                  ),
                  IdentifiersStub(
                    identifierType = TypeStub(
                      id = "sierra-identifier",
                      label = "Sierra identifier"
                    ),
                    value = "1093837"
                  )
                )
              )
            )
          )
        )

        whenReady(future) { _ shouldBe expectedWork }
      }
    }

    it("handles a work without any items") {
      withCatalogueSource { catalogueSource =>
        val future =
          catalogueSource.getWorkStub(id = CanonicalId("a2284uhb"))

        val expectedWork = WorkStub(id = "a2284uhb", items = List.empty)

        whenReady(future) { _ shouldBe expectedWork }
      }
    }

    it("handles a work where an item is not identified") {
      withCatalogueSource { catalogueSource =>
        val future =
          catalogueSource.getWorkStub(id = CanonicalId("a227dajt"))

        val expectedWork = WorkStub(
          id = "a227dajt",
          items = List(
            ItemStub(id = None, identifiers = None)
          )
        )

        whenReady(future) { _ shouldBe expectedWork }
      }
    }

    it("can search for an identifier") {
      withCatalogueSource { catalogueSource =>
        val future = catalogueSource.getSearchStub(
          identifier = SierraItemIdentifier(1093837)
        )

        val expectedSearch = SearchStub(
          totalResults = 2,
          results = List(
            WorkStub(
              id = "aczz5tm3",
              items = List(
                ItemStub(
                  id = Some("qkdy3h58"),
                  identifiers = Some(
                    List(
                      IdentifiersStub(
                        identifierType = TypeStub(
                          id = "sierra-system-number",
                          label = "Sierra system number"
                        ),
                        value = "i11088953"
                      ),
                      IdentifiersStub(
                        identifierType = TypeStub(
                          id = "sierra-identifier",
                          label = "Sierra identifier"
                        ),
                        value = "1108895"
                      )
                    )
                  )
                )
              )
            ),
            WorkStub(
              id = "ayzrznsz",
              items = List(
                ItemStub(
                  id = Some("q2knsrhh"),
                  identifiers = Some(
                    List(
                      IdentifiersStub(
                        identifierType = TypeStub(
                          id = "sierra-system-number",
                          label = "Sierra system number"
                        ),
                        value = "i10938370"
                      ),
                      IdentifiersStub(
                        identifierType = TypeStub(
                          id = "sierra-identifier",
                          label = "Sierra identifier"
                        ),
                        value = "1093837"
                      )
                    )
                  )
                )
              )
            )
          )
        )

        whenReady(future) { _ shouldBe expectedSearch }
      }
    }
  }
}
