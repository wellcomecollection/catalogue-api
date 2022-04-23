package weco.api.items.services

import akka.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpRequest,
  HttpResponse,
  StatusCodes,
  Uri
}
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.akka.fixtures.Akka
import weco.api.items.fixtures.ItemsApiGenerators
import weco.api.items.models.CatalogueWork
import weco.catalogue.internal_model.locations.{
  AccessCondition,
  AccessMethod,
  AccessStatus,
  PhysicalLocation
}
import weco.catalogue.internal_model.work.generators.WorkGenerators
import weco.fixtures.TestWith
import weco.http.client.{HttpGet, MemoryHttpClient}
import weco.sierra.generators.SierraIdentifierGenerators

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WorkLookupTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with WorkGenerators
    with Akka
    with ScalaFutures
    with IntegrationPatience
    with ItemsApiGenerators
    with SierraIdentifierGenerators {

  def withLookup[R](
    responses: Seq[(HttpRequest, HttpResponse)]
  )(testWith: TestWith[WorkLookup, R]): R =
    withActorSystem { implicit actorSystem =>
      val catalogueApiClient = new MemoryHttpClient(responses) with HttpGet {
        override val baseUri: Uri = Uri("http://catalogue:9001")
      }

      testWith(new WorkLookup(catalogueApiClient))
    }

  it("returns a work with matching ID") {
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

    // Note: this is deliberately a hard-coded JSON string rather than the
    // helpers we use in other tests so we can be sure we really can decode
    // the catalogue API JSON, and avoid any encoding bugs in the tests themselves.
    val responses = Seq(
      (
        catalogueWorkRequest(work.state.canonicalId),
        HttpResponse(
          entity = HttpEntity(
            contentType = ContentTypes.`application/json`,
            s"""
            {
              "id": "${work.state.canonicalId}",
              "title": "${work.data.title.get}",
              "alternativeTitles": [],
              "identifiers": [
                {
                  "identifierType": {
                    "id": "${work.state.sourceIdentifier.identifierType.id}",
                    "label": "${work.state.sourceIdentifier.identifierType.label}",
                    "type": "IdentifierType"
                  },
                  "value": "${work.state.sourceIdentifier.value}",
                  "type": "Identifier"
                }
              ],
              "items": [
                {
                  "id": "${physicalItem.id.canonicalId}",
                  "identifiers": [
                    {
                      "identifierType": {
                        "id": "${physicalItem.id.sourceIdentifier.identifierType.id}",
                        "label": "${physicalItem.id.sourceIdentifier.identifierType.label}",
                        "type": "IdentifierType"
                      },
                      "value": "${physicalItem.id.sourceIdentifier.value}",
                      "type": "Identifier"
                    }
                  ],
                  "locations": [
                    {
                      "locationType": {
                        "id": "${physicalItem.locations.head.locationType.id}",
                        "label": "${physicalItem.locations.head.locationType.label}",
                        "type": "LocationType"
                      },
                      "label": "${physicalItem.locations.head
              .asInstanceOf[PhysicalLocation]
              .label}",
                      "accessConditions": [
                        {
                          "method": {
                            "id": "not-requestable",
                            "label": "Not requestable",
                            "type": "AccessMethod"
                          },
                          "status": {
                            "id": "temporarily-unavailable",
                            "label": "Temporarily unavailable",
                            "type": "AccessStatus"
                          },
                          "type": "AccessCondition"
                        }
                      ],
                      "type": "PhysicalLocation"
                    }
                  ],
                  "type": "Item"
                }
              ],
              "availabilities": [
                {
                  "id": "in-library",
                  "label": "In the library",
                  "type": "Availability"
                }
              ]
            }
          """
          )
        )
      )
    )

    val future = withLookup(responses) {
      _.byCanonicalId(work.state.canonicalId)
    }

    whenReady(future) {
      _ shouldBe Right(CatalogueWork(work))
    }
  }

  it("returns NotFound if there is no such work") {
    val id = createCanonicalId

    val responses = Seq(
      (
        catalogueWorkRequest(id),
        catalogueErrorResponse(status = StatusCodes.NotFound)
      )
    )

    val future = withLookup(responses) {
      _.byCanonicalId(id)
    }

    whenReady(future) {
      _ shouldBe Left(WorkNotFoundError(id))
    }
  }

  it("returns Left[UnknownWorkError] if the API has an error") {
    val id = createCanonicalId

    val responses = Seq(
      (
        catalogueWorkRequest(id),
        catalogueErrorResponse(status = StatusCodes.InternalServerError)
      )
    )

    val future = withLookup(responses) {
      _.byCanonicalId(id)
    }

    whenReady(future) {
      _.left.value shouldBe a[UnknownWorkError]
    }
  }

  it("wraps an exception in the underlying client") {
    val err = new Throwable("BOOM!")

    val brokenClient = new MemoryHttpClient(responses = Seq()) with HttpGet {
      override val baseUri: Uri = Uri("http://catalogue:9001")

      override def get(
        path: Uri.Path,
        params: Map[String, String]
      ): Future[HttpResponse] =
        Future.failed(err)
    }

    val future = withActorSystem { implicit as =>
      val lookup = new WorkLookup(brokenClient)

      lookup.byCanonicalId(createCanonicalId)
    }

    whenReady(future) {
      _.left.value shouldBe a[UnknownWorkError]
    }
  }
}
