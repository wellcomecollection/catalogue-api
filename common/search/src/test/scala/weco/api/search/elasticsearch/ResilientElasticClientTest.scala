package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.{ElasticClient, ElasticRequest, Handler, HttpClient, HttpResponse}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ResilientElasticClientTest extends AnyFunSpec with Matchers with ScalaFutures {

  class MockHttpClient(responseFunction: ElasticRequest => Future[HttpResponse]) extends HttpClient {
    override def send(request: ElasticRequest, callback: Either[Throwable, HttpResponse] => Unit): Unit = {
      responseFunction(request).onComplete {
        case scala.util.Success(response) => callback(Right(response))
        case scala.util.Failure(exception) => callback(Left(exception))
      }
    }

    override def close(): Unit = {}
  }

  def createResponse(statusCode: Int, body: String = ""): HttpResponse = {
    HttpResponse(statusCode, Some(com.sksamuel.elastic4s.HttpEntity.StringEntity(body, None)), Map.empty)
  }

  // Dummy handler for string requests
  implicit val handler: Handler[String, String] = new Handler[String, String] {
    override def responseHandler: com.sksamuel.elastic4s.ResponseHandler[String] = new com.sksamuel.elastic4s.ResponseHandler[String] {
      override def handle(response: HttpResponse): Either[com.sksamuel.elastic4s.ElasticError, String] = Right("success")
    }
    override def build(t: String): ElasticRequest = ElasticRequest("GET", "/")
  }

  describe("ResilientElasticClient") {
    it("retries on 401") {
      var callCount = 0
      var factoryCallCount = 0
      val clientFactory = () => {
        factoryCallCount += 1
        val httpClient = new MockHttpClient(_ => {
          callCount += 1
          if (callCount == 1) {
            Future.successful(createResponse(401))
          } else {
            Future.successful(createResponse(200, "{}"))
          }
        })
        ElasticClient(httpClient)
      }

      val resilientClient = new ResilientElasticClient(clientFactory)
      
      val future = resilientClient.execute("test request")
      
      whenReady(future) { response =>
        response.status shouldBe 200
        callCount shouldBe 2
        factoryCallCount shouldBe 2 // Initial + Refresh
      }
    }

    it("retries on 403") {
      var callCount = 0
      var factoryCallCount = 0
      val clientFactory = () => {
        factoryCallCount += 1
        val httpClient = new MockHttpClient(_ => {
          callCount += 1
          if (callCount == 1) {
            Future.successful(createResponse(403))
          } else {
            Future.successful(createResponse(200, "{}"))
          }
        })
        ElasticClient(httpClient)
      }

      val resilientClient = new ResilientElasticClient(clientFactory)
      
      val future = resilientClient.execute("test request")
      
      whenReady(future) { response =>
        response.status shouldBe 200
        callCount shouldBe 2
        factoryCallCount shouldBe 2
      }
    }

    it("does not retry on 404") {
      var callCount = 0
      var factoryCallCount = 0
      val clientFactory = () => {
        factoryCallCount += 1
        val httpClient = new MockHttpClient(_ => {
          callCount += 1
          Future.successful(createResponse(404))
        })
        ElasticClient(httpClient)
      }

      val resilientClient = new ResilientElasticClient(clientFactory)
      
      val future = resilientClient.execute("test request")
      
      whenReady(future) { response =>
        response.status shouldBe 404
        callCount shouldBe 1
        factoryCallCount shouldBe 1
      }
    }

    it("does not retry on 500") {
      var callCount = 0
      var factoryCallCount = 0
      val clientFactory = () => {
        factoryCallCount += 1
        val httpClient = new MockHttpClient(_ => {
          callCount += 1
          Future.successful(createResponse(500))
        })
        ElasticClient(httpClient)
      }

      val resilientClient = new ResilientElasticClient(clientFactory)
      
      val future = resilientClient.execute("test request")
      
      whenReady(future) { response =>
        response.status shouldBe 500
        callCount shouldBe 1
        factoryCallCount shouldBe 1
      }
    }

    it("only retries once") {
      var callCount = 0
      var factoryCallCount = 0
      val clientFactory = () => {
        factoryCallCount += 1
        val httpClient = new MockHttpClient(_ => {
          callCount += 1
          Future.successful(createResponse(401))
        })
        ElasticClient(httpClient)
      }

      val resilientClient = new ResilientElasticClient(clientFactory)
      
      val future = resilientClient.execute("test request")
      
      whenReady(future) { response =>
        response.status shouldBe 401
        callCount shouldBe 2
        factoryCallCount shouldBe 2
      }
    }
  }
}
