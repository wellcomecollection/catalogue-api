package weco.api.search.rest

import akka.http.scaladsl.model.{StatusCodes, Uri}
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.index.IndexFixtures
import uk.ac.wellcome.models.work.generators.WorkGenerators
import uk.ac.wellcome.platform.api.models.ApiConfig
import weco.api.search.elasticsearch.ElasticLookup
import weco.api.search.fixtures.ResponseFixtures
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.work.{Work, WorkState}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class SingleWorkLookupTest
  extends AnyFunSpec
    with Matchers
    with EitherValues
    with IndexFixtures
    with WorkGenerators
    with ResponseFixtures {
  val publicRootUri = "https://example.wellcomecollection.org/api/works"
  val exampleContextUri = "https://example.wellcomecollection.org/context.json"

  val lookup = new SingleWorkLookup {
    override implicit val ec: ExecutionContext = global
    override implicit val elasticLookup: ElasticLookup[Work[WorkState.Indexed]] =
      new ElasticLookup()

    override implicit val apiConfig: ApiConfig =
      ApiConfig(
        publicRootUri = Uri("http://example.com/api"),
        defaultPageSize = 10,
        contextSuffix = "context.json"
      )

    override def context: String = exampleContextUri
  }

  it("finds a work") {
    val w = indexedWork()

    withLocalWorksIndex { index =>
      insertIntoElasticsearch(index, w)

      val future = lookup.lookupSingleWork(w.state.canonicalId)(index)

      whenReady(future) {
        _.right.value shouldBe w
      }
    }
  }

  it("returns a redirect if a Work is redirected") {
    val redirectedWork = indexedWork().redirected(
      IdState.Identified(
        canonicalId = createCanonicalId,
        sourceIdentifier = createSourceIdentifier
      )
    )
    val redirectId = redirectedWork.redirectTarget.canonicalId
    println(redirectId)

    withLocalWorksIndex { index =>
      insertIntoElasticsearch(index, redirectedWork)

      val future = lookup.lookupSingleWork(redirectedWork.state.canonicalId)(index)

      whenReady(future) { resp =>
        val route = resp.left.value
        assertRedirectResponse(route, path = s"/works/$redirectId") {
          StatusCodes.Found -> s"${apiConfig.publicRootPath}/$redirectId"
        }
      }
    }
  }
}
