package weco.api.items

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.items.fixtures.{ItemsApiFixture, ItemsApiGenerators}
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.identifiers.IdentifierType.SierraSystemNumber
import weco.catalogue.internal_model.identifiers.{
  CanonicalId,
  IdState,
  SourceIdentifier
}
import weco.catalogue.internal_model.locations.{
  AccessCondition,
  AccessMethod,
  AccessStatus
}
import weco.json.utils.JsonAssertions
import weco.sierra.generators.SierraIdentifierGenerators

import scala.util.{Failure, Try}

class ItemsApiFeatureTest
    extends AnyFunSpec
    with Matchers
    with ItemsApiFixture
    with JsonAssertions
    with IntegrationPatience
    with ItemsApiGenerators
    with SierraIdentifierGenerators {

  describe("look up the status of an item") {
    it("shows a user the items on a work") {
      val sierraItemNumber = createSierraItemNumber

      val temporarilyUnavailableOnline = AccessCondition(
        method = AccessMethod.NotRequestable,
        status = AccessStatus.TemporarilyUnavailable
      )

      val physicalItem = createPhysicalItemWith(
        sierraItemNumber = sierraItemNumber,
        accessCondition = temporarilyUnavailableOnline
      )

      val work = indexedWork().items(List(physicalItem))

      val availableItemResponses = Seq(
        (
          sierraItemRequest(sierraItemNumber),
          HttpResponse(
            entity = sierraItemResponse(
              sierraItemNumber = sierraItemNumber
            )
          )
        )
      )

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, work)

        withItemsApi(index, availableItemResponses) { _ =>
          val path = s"/works/${work.state.canonicalId}"

          val expectedJson =
            s"""
               |{
               |  "type" : "ItemsList",
               |  "totalResults" : 1,
               |  "results" : [
               |    {
               |      "id" : "${physicalItem.id.canonicalId}",
               |      "identifiers" : [
               |        {
               |          "identifierType" : {
               |            "id" : "sierra-system-number",
               |            "label" : "Sierra system number",
               |            "type" : "IdentifierType"
               |          },
               |          "value" : "${sierraItemNumber.withCheckDigit}",
               |          "type" : "Identifier"
               |        }
               |      ],
               |      "locations" : [
               |        {
               |          "locationType" : {
               |            "id" : "closed-stores",
               |            "label" : "Closed stores",
               |            "type" : "LocationType"
               |          },
               |          "label" : "locationLabel",
               |          "accessConditions" : [
               |            {
               |              "method" : {
               |                "id" : "online-request",
               |                "label" : "Online request",
               |                "type" : "AccessMethod"
               |              },
               |              "status": {
               |                "id" : "open",
               |                "label" : "Open",
               |                "type" : "AccessStatus"
               |              },
               |              "type" : "AccessCondition"
               |            }
               |          ],
               |          "type" : "PhysicalLocation"
               |        }
               |      ],
               |      "type" : "Item"
               |    }
               |  ]
               |}""".stripMargin

          whenGetRequestReady(path) { response =>
            response.status shouldBe StatusCodes.OK

            withStringEntity(response.entity) {
              assertJsonStringsAreEqual(_, expectedJson)
            }
          }
        }
      }
    }

    it("returns an empty list if a work has no items") {
      val work = indexedWork().items(List())

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, work)

        withItemsApi(index) { _ =>
          val path = s"/works/${work.state.canonicalId}"

          val expectedJson =
            s"""
               |{
               |  "type" : "ItemsList",
               |  "totalResults" : 0,
               |  "results" : [
               |  ]
               |}
              """.stripMargin

          whenGetRequestReady(path) { response =>
            response.status shouldBe StatusCodes.OK

            withStringEntity(response.entity) {
              assertJsonStringsAreEqual(_, expectedJson)
            }
          }
        }
      }
    }

    it("returns a 404 if there is no work with this ID") {
      val id = createCanonicalId

      withLocalWorksIndex { index =>
        withItemsApi(index) { _ =>
          val path = s"/works/$id"

          val expectedError =
            s"""
               |{
               |  "errorType": "http",
               |  "httpStatus": 404,
               |  "label": "Not Found",
               |  "description": "Work not found for identifier $id",
               |  "type": "Error"
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

    it("returns a 404 if the ID is not a valid canonical ID") {
      val id = randomAlphanumeric(length = 10)
      Try {
        CanonicalId(id)
      } shouldBe a[Failure[_]]

      withLocalWorksIndex { index =>
        withItemsApi(index) { _ =>
          val path = s"/works/$id"

          val expectedError =
            s"""
               |{
               |  "errorType": "http",
               |  "httpStatus": 404,
               |  "label": "Not Found",
               |  "description": "Work not found for identifier $id",
               |  "type": "Error"
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

    it("follows redirects in the works index") {
      val item = createIdentifiedItemWith(
        sourceIdentifier = SourceIdentifier(
          identifierType = SierraSystemNumber,
          value = "i16010176",
          ontologyType = "Item"
        )
      )

      val targetWork = indexedWork().items(List(item))
      val redirectedWork = indexedWork()
        .redirected(
          IdState.Identified(
            canonicalId = targetWork.state.canonicalId,
            sourceIdentifier = targetWork.state.sourceIdentifier
          )
        )

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, targetWork, redirectedWork)

        withItemsApi(index) { _ =>
          val path = s"/works/${redirectedWork.state.canonicalId}"

          whenGetRequestReady(path) { response =>
            response.status shouldBe StatusCodes.Found

            response.headers
              .filter { h =>
                h.name() == "Location"
              }
              .head
              .value() shouldBe s"/catalogue/works/${targetWork.state.canonicalId}"
          }
        }
      }
    }

    it("returns a 410 if the work is invisible") {
      val item = createIdentifiedItemWith(
        sourceIdentifier = SourceIdentifier(
          identifierType = SierraSystemNumber,
          value = "i16010176",
          ontologyType = "Item"
        )
      )

      val invisibleWork = indexedWork().items(List(item)).invisible()

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, invisibleWork)

        withItemsApi(index) { _ =>
          val path = s"/works/${invisibleWork.state.canonicalId}"

          val expectedError =
            s"""
               |{
               |  "errorType": "http",
               |  "httpStatus": 410,
               |  "label": "Gone",
               |  "description": "This work has been deleted",
               |  "type": "Error"
               |}""".stripMargin

          whenGetRequestReady(path) { response =>
            response.status shouldBe StatusCodes.Gone

            withStringEntity(response.entity) {
              assertJsonStringsAreEqual(_, expectedError)
            }
          }
        }
      }
    }

    it("returns a 410 if the work is deleted") {
      val item = createIdentifiedItemWith(
        sourceIdentifier = SourceIdentifier(
          identifierType = SierraSystemNumber,
          value = "i16010176",
          ontologyType = "Item"
        )
      )

      val deletedWork = indexedWork().items(List(item)).deleted()

      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, deletedWork)

        withItemsApi(index) { _ =>
          val path = s"/works/${deletedWork.state.canonicalId}"

          val expectedError =
            s"""
               |{
               |  "errorType": "http",
               |  "httpStatus": 410,
               |  "label": "Gone",
               |  "description": "This work has been deleted",
               |  "type": "Error"
               |}""".stripMargin

          whenGetRequestReady(path) { response =>
            response.status shouldBe StatusCodes.Gone

            withStringEntity(response.entity) {
              assertJsonStringsAreEqual(_, expectedError)
            }
          }
        }
      }
    }
  }
}
