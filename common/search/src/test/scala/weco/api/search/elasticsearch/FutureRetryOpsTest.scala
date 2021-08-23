package weco.api.search.elasticsearch

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FutureRetryOpsTest extends AnyFunSpec with Matchers with ScalaFutures {
  import FutureRetryOps._

  private def alwaysRetry(t: Throwable): Boolean =
    true

  private def neverRetry(t: Throwable): Boolean =
    false

  it("if N=1 is successful, it succeeds") {
    var callCount = 0

    def canOnlyBeCalledOnce(s: String): Future[String] = {
      callCount += 1

      if (callCount == 1) {
        Future.successful(s.toUpperCase)
      } else {
        Future.failed(new Throwable("BOOM!"))
      }
    }

    val retryableFunction = (canOnlyBeCalledOnce _).retry(maxAttempts = 3, isRetryable = neverRetry)

    whenReady(retryableFunction("hello")) {
      _ shouldBe "HELLO"
    }
  }

  it("if N=1–5 are retryable and N=6 is successful, it succeeds") {
    val maxAttempts = 6

    var callCount = 0

    def countCalls(s: String): Future[String] = {
      callCount += 1

      if (callCount > maxAttempts - 1) {
        Future.successful(s.toUpperCase)
      } else {
        Future.failed(new Throwable("BOOM!"))
      }
    }

    val retryableFunction = (countCalls _).retry(maxAttempts, isRetryable = alwaysRetry)

    whenReady(retryableFunction("hello")) {
      _ shouldBe "HELLO"
    }
  }

  it("if N=1 is retryable and N=2 isn't, it fails") {
    var callCount = 0
    val err = new Throwable("BOOM!")

    def failsAfterFirstCall(s: String): Future[String] = {
      callCount += 1

      if (callCount == 1) {
        Future.failed(new Throwable("retryable BOOM!"))
      } else {
        Future.failed(err)
      }
    }

    val retryableFunction = (failsAfterFirstCall _).retry(maxAttempts = 3, isRetryable = (t: Throwable) => t.getMessage.contains("retryable"))

    whenReady(retryableFunction("hello").failed) {
      _ shouldBe err
    }
  }

  it("if N=1 to maxAttempts are all retryable, it fails with the final error") {
    val maxAttempts = 5

    val err = new Throwable("BOOM!")

    def alwaysFails(s: String): Future[String] =
      Future.failed(err)

    val retryableFunction = (alwaysFails _).retry(maxAttempts, isRetryable = alwaysRetry)

    whenReady(retryableFunction("hello").failed) {
      _ shouldBe err
    }
  }

  it("makes one attempt by default") {
    var callCount = 0
    val err = new Throwable("BOOM!")

    def countCalls(s: String): Future[String] = {
      callCount += 1
      Future.failed(err)
    }

    val retryableFunction = (countCalls _).retry(isRetryable = alwaysRetry)

    whenReady(retryableFunction("hello").failed) {
      _ shouldBe err
    }

    callCount shouldBe 1
  }
}
