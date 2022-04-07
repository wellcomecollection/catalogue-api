package weco.api.requests.fixtures

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, Uri}
import com.sksamuel.elastic4s.Index
import org.scalatest.Suite
import weco.akka.fixtures.Akka
import weco.api.requests.services.{DisplayWorkResults, ItemLookup}
import weco.api.search.elasticsearch.ElasticsearchService
import weco.catalogue.display_model.models.{DisplayWork, WorksIncludes}
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.work.{Work, WorkState}
import weco.elasticsearch.test.fixtures.ElasticsearchFixtures
import weco.fixtures.TestWith
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}
import weco.http.json.DisplayJsonUtil
import weco.json.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

trait ItemLookupFixture extends ElasticsearchFixtures with Akka { this: Suite =>
  def withItemLookup[R](index: Index, responses: Seq[(HttpRequest, HttpResponse)])(testWith: TestWith[ItemLookup, R]): R =
    withActorSystem { implicit actorSystem =>
      val client = new MemoryHttpClient(responses) with HttpGet with HttpPost {
        override val baseUri: Uri = Uri("http://catalogue:9001")
      }

      testWith(
        new ItemLookup(client, new ElasticsearchService(elasticClient), index = index)
      )
    }

  def catalogueItemRequest(id: CanonicalId): HttpRequest =
    HttpRequest(
      uri = Uri(s"http://catalogue:9001/works?include=identifiers,items&identifiers=$id&pageSize=1")
    )

  import weco.catalogue.display_model.models.Implicits._

  def catalogueWorkResponse(works: Seq[Work.Visible[WorkState.Indexed]]): HttpResponse =
    HttpResponse(
      entity = HttpEntity(
        contentType = ContentTypes.`application/json`,
        DisplayJsonUtil.toJson(
          DisplayWorkResults(works.map(w => DisplayWork(w, includes = WorksIncludes.all)))
      ))
    )
}
