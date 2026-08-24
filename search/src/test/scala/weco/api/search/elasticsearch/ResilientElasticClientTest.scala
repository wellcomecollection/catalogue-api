package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.{
  ElasticClient,
  ElasticRequest,
  Handler,
  HttpClient,
  HttpResponse
}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}

import java.io.IOException
import java.time.Clock
import java.util.concurrent.{ConcurrentLinkedQueue, CountDownLatch}
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{blocking, Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

class ResilientElasticClientTest
    extends AnyFunSpec
    with Matchers
    with ScalaFutures
    with Eventually
    with IntegrationPatience
    with BeforeAndAfterAll {

  private def isAuthProbe(request: ElasticRequest): Boolean =
    request.endpoint == "/_security/_authenticate"

  // Counts real requests and auth probes separately, and records close()
  class MockHttpClient(responseFunction: ElasticRequest => Future[HttpResponse])
      extends HttpClient {
    val requestCount = new AtomicInteger(0)
    val probeCount = new AtomicInteger(0)
    @volatile var closed = false

    override def send(
      request: ElasticRequest,
      callback: Either[Throwable, HttpResponse] => Unit): Unit = {
      if (isAuthProbe(request)) probeCount.incrementAndGet()
      else requestCount.incrementAndGet()
      responseFunction(request).onComplete {
        case scala.util.Success(response)  => callback(Right(response))
        case scala.util.Failure(exception) => callback(Left(exception))
      }
    }

    override def close(): Unit = closed = true
  }

  def createResponse(statusCode: Int, body: String = ""): HttpResponse =
    HttpResponse(
      statusCode,
      Some(com.sksamuel.elastic4s.HttpEntity.StringEntity(body, None)),
      Map.empty)

  def respond(statusCode: Int): ElasticRequest => Future[HttpResponse] =
    _ => Future.successful(createResponse(statusCode))

  def respondWith(probeStatus: Int,
                  requestStatus: Int): ElasticRequest => Future[HttpResponse] =
    request =>
      Future.successful(
        createResponse(
          if (isAuthProbe(request)) probeStatus else requestStatus))

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

  implicit val clock: Clock = Clock.systemUTC()

  // Wraps each mock in a fresh ElasticClient: call n gets the nth mock, the last repeats
  def clientSequenceFactory(
    clients: MockHttpClient*): (() => ElasticClient, AtomicInteger) = {
    val calls = new AtomicInteger(0)
    val factory = () => {
      val n = calls.incrementAndGet()
      ElasticClient(clients(math.min(n, clients.size) - 1))
    }
    (factory, calls)
  }

  private val createdClients =
    new ConcurrentLinkedQueue[ResilientElasticClient]()

  def newResilientClient(factory: () => ElasticClient,
                         minRefreshIntervalMs: Long = 2000,
                         closeGraceMs: Long = 30000): ResilientElasticClient = {
    val resilientClient =
      new ResilientElasticClient(factory, minRefreshIntervalMs, closeGraceMs)
    createdClients.add(resilientClient)
    resilientClient
  }

  override def afterAll(): Unit = {
    createdClients.forEach(c => c.close())
    super.afterAll()
  }

  describe("ResilientElasticClient") {
    it("retries on 401 against a refreshed client") {
      val broken = new MockHttpClient(respond(401))
      val healthy = new MockHttpClient(respond(200))
      val (factory, factoryCallCount) = clientSequenceFactory(broken, healthy)
      val resilientClient = newResilientClient(factory)

      val future = resilientClient.execute("test request")
      whenReady(future) { response =>
        response.status shouldBe 200
        factoryCallCount.get shouldBe 2 // Initial + refresh
        broken.requestCount.get shouldBe 1
        healthy.requestCount.get shouldBe 1
        healthy.probeCount.get shouldBe 1 // Validated before swap
      }
    }

    it("retries on 403 against a refreshed client") {
      val broken = new MockHttpClient(respond(403))
      val healthy = new MockHttpClient(respond(200))
      val (factory, factoryCallCount) = clientSequenceFactory(broken, healthy)
      val resilientClient = newResilientClient(factory)

      val future = resilientClient.execute("test request")
      whenReady(future) { response =>
        response.status shouldBe 200
        factoryCallCount.get shouldBe 2
        broken.requestCount.get shouldBe 1
        healthy.requestCount.get shouldBe 1
      }
    }

    it("does not retry on 404") {
      val httpClient = new MockHttpClient(respond(404))
      val (factory, factoryCallCount) = clientSequenceFactory(httpClient)
      val resilientClient = newResilientClient(factory)

      val future = resilientClient.execute("test request")
      whenReady(future) { response =>
        response.status shouldBe 404
        httpClient.requestCount.get shouldBe 1
        factoryCallCount.get shouldBe 1
      }
    }

    it("does not retry on 500") {
      val httpClient = new MockHttpClient(respond(500))
      val (factory, factoryCallCount) = clientSequenceFactory(httpClient)
      val resilientClient = newResilientClient(factory)

      val future = resilientClient.execute("test request")
      whenReady(future) { response =>
        response.status shouldBe 500
        httpClient.requestCount.get shouldBe 1
        factoryCallCount.get shouldBe 1
      }
    }

    it("returns the original 401 without a retry if the refresh is rejected") {
      val original = new MockHttpClient(respond(401))
      val deadCandidate = new MockHttpClient(respond(401))
      val (factory, factoryCallCount) =
        clientSequenceFactory(original, deadCandidate)
      val resilientClient = newResilientClient(factory)

      val future = resilientClient.execute("test request")
      whenReady(future) { response =>
        response.status shouldBe 401
        factoryCallCount.get shouldBe 2
        original.requestCount.get shouldBe 1 // No futile second execution
        deadCandidate.probeCount.get shouldBe 1
        deadCandidate.requestCount.get shouldBe 0 // Never swapped in
        deadCandidate.closed shouldBe true
      }
    }

    it("accepts a swap when the probe is inconclusive") {
      val broken = new MockHttpClient(respond(401))
      // 5xx from the probe is not evidence of a bad key
      val candidate = new MockHttpClient(respondWith(500, 200))
      val (factory, factoryCallCount) = clientSequenceFactory(broken, candidate)
      val resilientClient = newResilientClient(factory)

      val future = resilientClient.execute("test request")
      whenReady(future) { response =>
        response.status shouldBe 200
        factoryCallCount.get shouldBe 2
        candidate.requestCount.get shouldBe 1
      }
    }

    it("propagates a transport failure without refreshing") {
      val boom = new IOException("connection reset")
      val broken = new MockHttpClient(_ => Future.failed(boom))
      val (factory, factoryCallCount) = clientSequenceFactory(broken)
      val resilientClient = newResilientClient(factory)

      val future = resilientClient.execute("test request")
      whenReady(future.failed) { e =>
        e should be theSameInstanceAs boom
        broken.requestCount.get shouldBe 1 // Fail fast, no retry
        factoryCallCount.get shouldBe 1 // No refresh triggered
      }
    }

    it("retries a transport failure on the current client after a swap") {
      val firstRequestGate = Promise[HttpResponse]()
      val brokenSeen = new AtomicInteger(0)
      // First request is held open; later requests fail fast with 401
      val broken = new MockHttpClient(
        _ =>
          if (brokenSeen.incrementAndGet() == 1) firstRequestGate.future
          else Future.successful(createResponse(401)))
      val healthy = new MockHttpClient(respond(200))
      val (factory, factoryCallCount) = clientSequenceFactory(broken, healthy)
      val resilientClient = newResilientClient(factory)

      val heldRequest = resilientClient.execute("held request")
      eventually { broken.requestCount.get shouldBe 1 }

      // This request triggers the refresh and swaps in the healthy client
      val refreshingRequest = resilientClient.execute("fast request")
      whenReady(refreshingRequest) { response =>
        response.status shouldBe 200
      }

      // The held request now fails at transport level: its client was closed under it
      firstRequestGate.failure(new IOException("connection closed"))
      whenReady(heldRequest) { response =>
        response.status shouldBe 200
      }
      factoryCallCount.get shouldBe 2 // The failure did not trigger a refresh
      healthy.requestCount.get shouldBe 2
    }

    it("refreshes again if the retry after a mid-flight swap gets a 401") {
      val firstRequestGate = Promise[HttpResponse]()
      val firstSeen = new AtomicInteger(0)
      // First request is held open; later requests fail fast with 401
      val first = new MockHttpClient(
        _ =>
          if (firstSeen.incrementAndGet() == 1) firstRequestGate.future
          else Future.successful(createResponse(401)))
      // Second client authenticates but still 401s requests
      val second = new MockHttpClient(respondWith(200, 401))
      val third = new MockHttpClient(respond(200))
      val (factory, factoryCallCount) =
        clientSequenceFactory(first, second, third)
      val resilientClient =
        newResilientClient(factory, minRefreshIntervalMs = 1)

      val heldRequest = resilientClient.execute("held request")
      eventually { first.requestCount.get shouldBe 1 }

      // Swap in the second client via a fast 401
      val refreshingRequest = resilientClient.execute("fast request")
      whenReady(refreshingRequest) { response =>
        response.status shouldBe 401 // Retried on second, which also 401s
      }
      factoryCallCount.get shouldBe 2

      // Let the cooldown lapse so the recursive retry can refresh again
      Thread.sleep(10)

      // The held request's transport-failure retry 401s on second, so it
      // must go through the full refresh path and succeed on third
      firstRequestGate.failure(new IOException("connection closed"))
      whenReady(heldRequest) { response =>
        response.status shouldBe 200
      }
      factoryCallCount.get shouldBe 3
      third.requestCount.get shouldBe 1
    }

    it("retries against the current client when it was refreshed mid-request") {
      val firstRequestGate = Promise[HttpResponse]()
      val brokenSeen = new AtomicInteger(0)
      // First request is held open; later requests fail fast
      val broken = new MockHttpClient(
        _ =>
          if (brokenSeen.incrementAndGet() == 1) firstRequestGate.future
          else Future.successful(createResponse(401)))
      val healthy = new MockHttpClient(respond(200))
      val (factory, factoryCallCount) = clientSequenceFactory(broken, healthy)

      // Huge cooldown: the held request must not be able to refresh again
      val resilientClient =
        newResilientClient(factory, minRefreshIntervalMs = 600000)

      val heldRequest = resilientClient.execute("held request")
      eventually { broken.requestCount.get shouldBe 1 }

      // This request triggers the refresh and succeeds on the new client
      val refreshingRequest = resilientClient.execute("fast request")
      whenReady(refreshingRequest) { response =>
        response.status shouldBe 200
      }
      factoryCallCount.get shouldBe 2

      // The held request now fails with 401 but must retry on the refreshed client
      firstRequestGate.success(createResponse(401))
      whenReady(heldRequest) { response =>
        response.status shouldBe 200
      }
      factoryCallCount.get shouldBe 2
      broken.requestCount.get shouldBe 2 // No retry against the stale client
      healthy.requestCount.get shouldBe 2
    }

    it("recovers once a dead secret starts serving a working key") {
      @volatile var secretIsDead = true
      val original = new MockHttpClient(respond(401))
      val rebuilt = new ConcurrentLinkedQueue[MockHttpClient]()
      val factoryCallCount = new AtomicInteger(0)

      val factory = () =>
        if (factoryCallCount.incrementAndGet() == 1) ElasticClient(original)
        else {
          // A client built from a dead key stays dead
          val mock =
            if (secretIsDead) new MockHttpClient(respond(401))
            else new MockHttpClient(respond(200))
          rebuilt.add(mock)
          ElasticClient(mock)
      }

      val resilientClient =
        newResilientClient(factory, minRefreshIntervalMs = 100)

      // While the secret is dead the rebuilt client fails validation and is discarded
      val deadFuture = resilientClient.execute("request 1")
      whenReady(deadFuture) { response =>
        response.status shouldBe 401
        factoryCallCount.get shouldBe 2
      }
      original.requestCount.get shouldBe 1 // Original 401 returned, no retry
      val deadRebuild = rebuilt.peek()
      deadRebuild.probeCount.get shouldBe 1
      deadRebuild.requestCount.get shouldBe 0 // Never swapped in
      deadRebuild.closed shouldBe true

      // After the cooldown, a refresh with a working key heals the client
      Thread.sleep(150)
      secretIsDead = false
      val healedFuture = resilientClient.execute("request 2")
      whenReady(healedFuture) { response =>
        response.status shouldBe 200
        factoryCallCount.get shouldBe 3
      }
      original.closed shouldBe false // Still in its close grace period
    }

    it("does not close the old client immediately after a refresh") {
      val broken = new MockHttpClient(respond(401))
      val healthy = new MockHttpClient(respond(200))
      val (factory, _) = clientSequenceFactory(broken, healthy)
      val resilientClient = newResilientClient(factory)

      val future = resilientClient.execute("test request")
      whenReady(future) { response =>
        response.status shouldBe 200
      }
      broken.closed shouldBe false
    }

    it("closes the old client after the grace period") {
      val broken = new MockHttpClient(respond(401))
      val healthy = new MockHttpClient(respond(200))
      val (factory, _) = clientSequenceFactory(broken, healthy)
      val resilientClient = newResilientClient(factory, closeGraceMs = 50)

      val future = resilientClient.execute("test request")
      whenReady(future) { response =>
        response.status shouldBe 200
      }
      eventually { broken.closed shouldBe true }
    }

    it("closes a pending old client as soon as another swap happens") {
      val first = new MockHttpClient(respond(401))
      // Second client authenticates but still 401s requests, forcing another swap
      val second = new MockHttpClient(respondWith(200, 401))
      val third = new MockHttpClient(respond(200))
      val (factory, factoryCallCount) =
        clientSequenceFactory(first, second, third)
      val resilientClient =
        newResilientClient(factory, minRefreshIntervalMs = 1)

      val future1 = resilientClient.execute("request 1")
      whenReady(future1) { response =>
        response.status shouldBe 401 // Retried on second, which also 401s
      }
      first.closed shouldBe false // Deferred

      Thread.sleep(10)

      val future2 = resilientClient.execute("request 2")
      whenReady(future2) { response =>
        response.status shouldBe 200
      }
      factoryCallCount.get shouldBe 3
      first.closed shouldBe true // Displaced by the newer pending close
      second.closed shouldBe false // Now the one deferred
    }

    it("closes the current and pending clients on close()") {
      val broken = new MockHttpClient(respond(401))
      val healthy = new MockHttpClient(respond(200))
      val (factory, _) = clientSequenceFactory(broken, healthy)
      val resilientClient = newResilientClient(factory)

      val future = resilientClient.execute("test request")
      whenReady(future) { response =>
        response.status shouldBe 200
      }

      resilientClient.close()
      broken.closed shouldBe true
      healthy.closed shouldBe true
    }

    it("throttles refresh requests within cooldown period") {
      val factoryCallCount = new AtomicInteger(0)
      val factory = () => {
        factoryCallCount.incrementAndGet()
        ElasticClient(new MockHttpClient(respond(401)))
      }

      // Use a very short cooldown to test throttling without actual delays
      val resilientClient =
        newResilientClient(factory, minRefreshIntervalMs = 100)

      // First request - should refresh
      val future1 = resilientClient.execute("test request 1")
      whenReady(future1) { response =>
        response.status shouldBe 401
        factoryCallCount.get shouldBe 2 // Initial + first refresh
      }

      // Second request immediately after (within cooldown) - should NOT refresh
      val future2 = resilientClient.execute("test request 2")
      whenReady(future2) { response =>
        response.status shouldBe 401
        factoryCallCount.get shouldBe 2 // No new refresh
      }

      // Wait for cooldown to expire
      Thread.sleep(150)

      // Third request after cooldown - should refresh
      val oldFactoryCallCount = factoryCallCount.get
      val future3 = resilientClient.execute("test request 3")
      whenReady(future3) { response =>
        response.status shouldBe 401
        factoryCallCount.get shouldBe oldFactoryCallCount + 1 // New refresh
      }
    }

    it("allows configurable cooldown period") {
      val factoryCallCount = new AtomicInteger(0)
      val factory = () => {
        factoryCallCount.incrementAndGet()
        ElasticClient(new MockHttpClient(respond(401)))
      }

      // Use a very short cooldown of 50ms
      val resilientClient =
        newResilientClient(factory, minRefreshIntervalMs = 50)

      // First request - should refresh
      val future1 = resilientClient.execute("test request 1")
      whenReady(future1) { _ =>
        factoryCallCount.get shouldBe 2
      }

      // Wait for cooldown to expire
      Thread.sleep(100)

      // Second request after cooldown - should refresh
      val future2 = resilientClient.execute("test request 2")
      whenReady(future2) { _ =>
        factoryCallCount.get shouldBe 3 // Another refresh
      }
    }

    it("coalesces concurrent 401 errors onto one refresh") {
      // Hold every request until both concurrent requests have arrived
      val bothArrived = new CountDownLatch(2)
      val broken = new MockHttpClient(_ =>
        Future {
          bothArrived.countDown()
          blocking(bothArrived.await())
          createResponse(401)
      })
      val healthy = new MockHttpClient(respond(200))
      val (factory, factoryCallCount) = clientSequenceFactory(broken, healthy)
      val resilientClient = newResilientClient(factory)

      val futures = Future.sequence(
        (1 to 2).map(_ => resilientClient.execute("concurrent request"))
      )

      whenReady(futures) { responses =>
        responses.foreach { response =>
          response.status shouldBe 200
        }
      }

      // Both 401s share a single refresh: 1 initial + 1 refresh
      factoryCallCount.get shouldBe 2
      broken.requestCount.get shouldBe 2
      healthy.requestCount.get shouldBe 2
    }
  }
}
