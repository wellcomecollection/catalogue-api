package weco.api.items.services

import akka.http.scaladsl.model._
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.akka.fixtures.Akka
import weco.api.items.fixtures.ItemsApiGenerators
import weco.api.stacks.models.{
  CatalogueAccessMethod,
  CatalogueAccessStatus,
  CatalogueWork
}
import weco.catalogue.display_model.identifiers.DisplayIdentifier
import weco.catalogue.display_model.locations.{
  DisplayAccessCondition,
  DisplayLocationType,
  DisplayPhysicalLocation
}
import weco.catalogue.display_model.work.DisplayItem
import weco.catalogue.internal_model.generators.IdentifiersGenerators
import weco.catalogue.internal_model.identifiers.{
  IdentifierType,
  SourceIdentifier
}
import weco.fixtures.TestWith
import weco.http.client.{HttpGet, MemoryHttpClient}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WorkLookupTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with Akka
    with ScalaFutures
    with IdentifiersGenerators
    with ItemsApiGenerators {

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
    val canonicalId = createCanonicalId
    val title = "a test item"
    val identifierType = IdentifierType.MiroImageNumber
    val sourceIdentifier = randomAlphanumeric()
    val itemId = randomAlphanumeric()
    val itemNumber = "i52945510"
    val locationLabel = "where the item is"

    // Note: this is deliberately a hard-coded JSON string rather than the
    // helpers we use in other tests so we can be sure we really can decode
    // the catalogue API JSON, and avoid any encoding bugs in the tests themselves.
    val responses = Seq(
      (
        catalogueWorkRequest(canonicalId),
        HttpResponse(
          entity = HttpEntity(
            contentType = ContentTypes.`application/json`,
            s"""
            {
              "id": "$canonicalId",
              "title": "$title",
              "alternativeTitles": [],
              "identifiers": [
                {
                  "identifierType": {
                    "id": "${identifierType.id}",
                    "label": "${identifierType.label}",
                    "type": "IdentifierType"
                  },
                  "value": "$sourceIdentifier",
                  "type": "Identifier"
                }
              ],
              "items": [
                {
                  "id": "$itemId",
                  "identifiers": [
                    {
                      "identifierType": {
                        "id": "sierra-system-number",
                        "label": "Sierra system number",
                        "type": "IdentifierType"
                      },
                      "value": "$itemNumber",
                      "type": "Identifier"
                    }
                  ],
                  "locations": [
                    {
                      "locationType": {
                        "id": "closed-stores",
                        "label": "Closed stores",
                        "type": "LocationType"
                      },
                      "label": "$locationLabel",
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
      _.byCanonicalId(canonicalId)
    }

    whenReady(future) {
      _ shouldBe Right(
        CatalogueWork(
          id = canonicalId.underlying,
          title = Some(title),
          identifiers = List(
            DisplayIdentifier(
              sourceIdentifier = SourceIdentifier(
                identifierType = identifierType,
                value = sourceIdentifier,
                ontologyType = "Work"
              )
            )
          ),
          items = List(
            DisplayItem(
              id = Some(itemId),
              identifiers = List(
                DisplayIdentifier(
                  sourceIdentifier = SourceIdentifier(
                    identifierType = IdentifierType.SierraSystemNumber,
                    value = itemNumber,
                    ontologyType = "Item"
                  )
                )
              ),
              locations = List(
                DisplayPhysicalLocation(
                  label = locationLabel,
                  locationType = DisplayLocationType(
                    id = "closed-stores",
                    label = "Closed stores"
                  ),
                  accessConditions = List(
                    DisplayAccessCondition(
                      status = CatalogueAccessStatus.TemporarilyUnavailable,
                      method = CatalogueAccessMethod.NotRequestable
                    )
                  )
                )
              )
            )
          )
        )
      )
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
