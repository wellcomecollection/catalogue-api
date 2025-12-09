package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.{
  ElasticClient,
  ElasticRequest,
  Handler,
  HttpClient,
  HttpResponse
}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import java.time.Clock
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ResilientElasticClientTest
    extends AnyFunSpec
    with Matchers
    with ScalaFutures {

  class MockHttpClient(responseFunction: ElasticRequest => Future[HttpResponse])
      extends HttpClient {
    override def send(request: ElasticRequest,
                      callback: Either[Throwable, HttpResponse] => Unit): Unit =
      responseFunction(request).onComplete {
        case scala.util.Success(response)  => callback(Right(response))
        case scala.util.Failure(exception) => callback(Left(exception))
      }

    override def close(): Unit = {}
  }

  def createResponse(statusCode: Int, body: String = ""): HttpResponse =
    HttpResponse(
      statusCode,
      Some(com.sksamuel.elastic4s.HttpEntity.StringEntity(body, None)),
      Map.empty)

  // Dummy handler for string requests
  implicit val handler: Handler[String, String] = new Handler[String, String] {
    override def responseHandler
      : com.sksamuel.elastic4s.ResponseHandler[String] =
      new com.sksamuel.elastic4s.ResponseHandler[String] {
        override def handle(response: HttpResponse)
          : Either[com.sksamuel.elastic4s.ElasticError, String] =
          Right("success")
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

      implicit val clock = Clock.systemUTC()
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

      implicit val clock = Clock.systemUTC()
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

      implicit val clock = Clock.systemUTC()
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

      implicit val clock = Clock.systemUTC()
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

      implicit val clock = Clock.systemUTC()
      val resilientClient = new ResilientElasticClient(clientFactory)

      val future = resilientClient.execute("test request")

      whenReady(future) { response =>
        response.status shouldBe 401
        callCount shouldBe 2
        factoryCallCount shouldBe 2
      }
    }

    it("throttles refresh requests within cooldown period") {
      var callCount = 0
      var factoryCallCount = 0

      val clientFactory = () => {
        factoryCallCount += 1
        val httpClient = new MockHttpClient(_ => {
          callCount += 1
          // Always return 401 to trigger refresh
          Future.successful(createResponse(401))
        })
        ElasticClient(httpClient)
      }

      implicit val clock = Clock.systemUTC()
      // Use a very short cooldown to test throttling without actual delays
      val resilientClient =
        new ResilientElasticClient(clientFactory, minRefreshIntervalMs = 100)

      // First request - should refresh
      var future = resilientClient.execute("test request 1")
      whenReady(future) { response =>
        response.status shouldBe 401
        factoryCallCount shouldBe 2 // Initial + first refresh
      }

      // Second request immediately after (within cooldown) - should NOT refresh
      future = resilientClient.execute("test request 2")
      whenReady(future) { response =>
        response.status shouldBe 401
        factoryCallCount shouldBe 2 // No new refresh
      }

      // Wait for cooldown to expire
      Thread.sleep(150)

      // Third request after cooldown - should refresh
      val oldFactoryCallCount = factoryCallCount
      future = resilientClient.execute("test request 3")
      whenReady(future) { response =>
        response.status shouldBe 401
        factoryCallCount shouldBe oldFactoryCallCount + 1 // New refresh
      }
    }

    it("allows configurable cooldown period") {
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

      implicit val clock = Clock.systemUTC()
      // Use a very short cooldown of 50ms
      val resilientClient =
        new ResilientElasticClient(clientFactory, minRefreshIntervalMs = 50)

      // First request - should refresh
      var future = resilientClient.execute("test request 1")
      whenReady(future) { response =>
        factoryCallCount shouldBe 2
      }

      // Wait for cooldown to expire
      Thread.sleep(100)

      // Second request after cooldown - should refresh
      future = resilientClient.execute("test request 2")
      whenReady(future) { response =>
        factoryCallCount shouldBe 3 // Another refresh
      }
    }

    it("only one thread refreshes during concurrent 401 errors") {
      var callCount = 0
      var factoryCallCount = 0
      var concurrentCallsAtPeak = 0
      val concurrentCallsLock = new Object()

      val clientFactory = () => {
        factoryCallCount += 1
        val httpClient = new MockHttpClient(_ => {
          concurrentCallsLock.synchronized {
            callCount += 1
            concurrentCallsAtPeak = Math.max(concurrentCallsAtPeak, callCount)
          }
          // Small delay to allow concurrent execution
          Thread.sleep(10)
          concurrentCallsLock.synchronized {
            callCount -= 1
          }
          Future.successful(createResponse(401))
        })
        ElasticClient(httpClient)
      }

      implicit val clock = Clock.systemUTC()
      val resilientClient = new ResilientElasticClient(clientFactory)

      // Create concurrent requests that all get 401
      // Using Future.sequence ensures they execute concurrently
      val futures = Future.sequence(
        (1 to 3).map(_ => resilientClient.execute("concurrent request"))
      )

      whenReady(futures) { responses =>
        responses.foreach { response =>
          response.status shouldBe 401
        }
      }

      // Verify concurrent execution happened (at least 2 concurrent calls)
      concurrentCallsAtPeak should be >= 2

      // Even though we had multiple concurrent 401s, factory should only be called:
      // 1 initial + 1 first refresh = 2
      // The other threads should see the client already refreshed
      factoryCallCount shouldBe 2
    }
  }
}
