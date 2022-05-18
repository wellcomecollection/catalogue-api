package weco.api.requests.fixtures

import akka.http.scaladsl.model._
import weco.akka.fixtures.Akka
import weco.api.requests.services.ItemLookup
import weco.catalogue.internal_model.identifiers.{CanonicalId, SourceIdentifier}
import weco.fixtures.TestWith
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}
import weco.http.fixtures.HttpFixtures

import scala.concurrent.ExecutionContext.Implicits.global

trait ItemLookupFixture extends Akka with HttpFixtures {
  def withItemLookup[R](
    responses: Seq[(HttpRequest, HttpResponse)]
  )(testWith: TestWith[ItemLookup, R]): R =
    withActorSystem { implicit actorSystem =>
      val client = new MemoryHttpClient(responses) with HttpGet with HttpPost {
        override val baseUri: Uri = Uri("http://catalogue:9001")
      }

      testWith(new ItemLookup(client))
    }

  def catalogueItemRequest(id: CanonicalId): HttpRequest =
    HttpRequest(
      uri = Uri(
        s"http://catalogue:9001/works?include=identifiers,items&items=$id&pageSize=1"
      )
    )

  def catalogueItemsRequest(ids: String*): HttpRequest =
    HttpRequest(
      uri = Uri(
        s"http://catalogue:9001/works?include=identifiers,items&items.identifiers=${ids.mkString(",")}&pageSize=100"
      )
    )

  def catalogueSourceIdsRequest(ids: SourceIdentifier*): HttpRequest =
    catalogueItemsRequest(ids.map(_.value): _*)
}
