package uk.ac.wellcome.platform.api.items

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.work.generators.{ItemsGenerators, WorkGenerators}
import uk.ac.wellcome.platform.api.items.fixtures.ItemsApiFixture
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraIdentifier
import weco.catalogue.internal_model.identifiers.SourceIdentifier
import weco.http.fixtures.HttpFixtures

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits

class ItemsApiFeatureTest
    extends AnyFunSpec
    with Matchers
    with ItemsApiFixture
    with JsonAssertions
    with IntegrationPatience
    with WorkGenerators
    with ItemsGenerators
    with HttpFixtures {

  describe("items") {
    it("shows a user the items on a work") {
      val item =
        createIdentifiedItemWith(
          sourceIdentifier = SourceIdentifier(
            identifierType = SierraIdentifier,
            value = "1601017",
            ontologyType = "Item"
          )
        )

      val work = indexedWork().items(List(item))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, work)

        withItemsApi(index) { _ =>
          val path = s"/works/${work.state.canonicalId}"

          val expectedJson =
            s"""
               |{
               |  "id" : "${work.state.canonicalId}",
               |  "items" : [
               |    {
               |      "id" : "${item.id.canonicalId}",
               |      "locations" : [
               |      ],
               |      "status" : {
               |        "id" : "available",
               |        "label" : "Available",
               |        "type": "ItemStatus"
               |      },
               |      "type": "Item"
               |    }
               |  ],
               |  "type": "Work"
               |}""".stripMargin

          whenGetRequestReady(path) { response =>
            response.status shouldBe StatusCodes.OK

            withStringEntity(response.entity) { actualJson =>
              assertJsonStringsAreEqual(actualJson, expectedJson)
            }
          }
        }
      }
    }

    it("returns an empty list if a work has no items") {
      val work = indexedWork()

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, work)

        withItemsApi(index) { _ =>
          val path = s"/works/${work.id}"

          val expectedJson =
            s"""
               |{
               |  "id" : "${work.id}",
               |  "items" : [ ],
               |  "type": "Work"
               |}""".stripMargin

          whenGetRequestReady(path) { response =>
            response.status shouldBe StatusCodes.OK

            withStringEntity(response.entity) { actualJson =>
              assertJsonStringsAreEqual(actualJson, expectedJson)
            }
          }
        }
      }
    }

    it("returns an empty list if a work has no identified items") {
      val work = indexedWork()
        .items(List(createUnidentifiableItem))

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, work)

        withItemsApi(index) { _ =>
          val path = s"/works/${work.id}"

          val expectedJson =
            s"""
               |{
               |  "id" : "${work.id}",
               |  "items" : [ ],
               |  "type": "Work"
               |}""".stripMargin

          whenGetRequestReady(path) { response =>
            response.status shouldBe StatusCodes.OK

            withStringEntity(response.entity) { actualJson =>
              assertJsonStringsAreEqual(actualJson, expectedJson)
            }
          }
        }
      }
    }

    // This is a test case we need, but we shouldn't enable it until we
    // have the WorksService working -- so we can detect the absence of a work.
    it("returns a 404 if the ID is not a canonical ID") {
      withItemsApi(createIndex) { _ =>
        val id = randomAlphanumeric()
        val path = s"/works/$id"

        val expectedError =
          s"""
             |{
             |  "errorType": "http",
             |  "httpStatus": 404,
             |  "label": "Not Found",
             |  "description": "Work not found for identifier $id",
             |  "type": "Error",
             |  "@context": "https://localhostcatalogue/context.json"
             |}""".stripMargin

        whenGetRequestReady(path) { response =>
          response.status shouldBe StatusCodes.NotFound

          withStringEntity(response.entity) {
            assertJsonStringsAreEqual(_, expectedError)
          }
        }
      }
    }
  }

  override implicit lazy val ec: ExecutionContext = Implicits.global
}
