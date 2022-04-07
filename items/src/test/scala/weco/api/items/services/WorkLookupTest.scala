package weco.api.items.services

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.akka.fixtures.Akka
import weco.api.items.fixtures.ItemsApiGenerators
import weco.catalogue.display_model.models.{DisplayWork, WorksIncludes}
import weco.catalogue.internal_model.work.generators.WorkGenerators
import weco.fixtures.TestWith
import weco.http.client.{HttpGet, MemoryHttpClient}

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
    val work = indexedWork()

    val responses = Seq(
      (
        catalogueWorkRequest(work.state.canonicalId),
        catalogueWorkResponse(work)
      )
    )

    val future = withLookup(responses) {
      _.byCanonicalId(work.state.canonicalId)
    }

    whenReady(future) {
      _ shouldBe Right(DisplayWork(work, includes = WorksIncludes.all))
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
