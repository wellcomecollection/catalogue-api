package weco.api.requests.fixtures

import org.apache.pekko.http.scaladsl.model._
import weco.pekko.fixtures.Pekko
import weco.api.requests.services.ItemLookup
import weco.catalogue.display_model.identifiers.DisplayIdentifier
import weco.fixtures.TestWith
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}
import weco.http.fixtures.HttpFixtures

import scala.concurrent.ExecutionContext.Implicits.global

trait ItemLookupFixture extends Pekko with HttpFixtures {
  def withItemLookup[R](
    responses: Seq[(HttpRequest, HttpResponse)]
  )(testWith: TestWith[ItemLookup, R]): R =
    withActorSystem { implicit actorSystem =>
      val client = new MemoryHttpClient(responses) with HttpGet with HttpPost {
        override val baseUri: Uri = Uri("http://catalogue:9001")
      }

      testWith(new ItemLookup(client))
    }

  def catalogueItemRequest(id: String): HttpRequest =
    HttpRequest(
      uri = Uri(
        s"http://catalogue:9001/works?include=identifiers,items&items=$id&pageSize=1"
      )
    )

  def catalogueItemsRequest(page: Int, ids: String*): HttpRequest =
    HttpRequest(
      uri = Uri(
        s"http://catalogue:9001/works?include=identifiers,items&items.identifiers=${ids
          .mkString(",")}&pageSize=100&page=$page"
      )
    )

  def catalogueSourceIdsRequest(
    page: Int,
    ids: DisplayIdentifier*
  ): HttpRequest =
    catalogueItemsRequest(page, ids.map(_.value): _*)
}
