package weco.api.search.elasticsearch

import grizzled.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}

/** Retry an operation which may return a `Throwable`.
  *
  * It tries the function up to `maxAttempts` times.  If at any point
  * the function returns a successful Future or fails with a
  * non-retryable Throwable, it finishes immediately.
  *
  */
object FutureRetryOps extends Logging {
  implicit class Retry[In, Out](f: In => Future[Out]) {

    def retry(maxAttempts: Int = 1, isRetryable: Throwable => Boolean)(implicit ec: ExecutionContext): In => Future[Out] =
      (in: In) => retryInternal(maxAttempts, isRetryable)(in)

    private def retryInternal(remainingAttempts: Int, isRetryable: Throwable => Boolean)(
      in: In)(implicit ec: ExecutionContext): Future[Out] =
      f(in)
        .map { out =>
          debug(s"Success: retryable operation for in=$in succeeded")
          out
        }
        .recoverWith {
          case t if isRetryable(t) =>
            debug(
              s"Retryable error: remaining attempts = $remainingAttempts for in=$in after throwable $t")
            if (remainingAttempts == 1) {
              debug(s"Retryable error: marking operation as failed with $t")
              Future.failed(t)
            } else {
              assert(remainingAttempts > 0)
              debug(s"Retryable error: retrying operation with $t")
              retryInternal(remainingAttempts - 1, isRetryable)(in)
            }

          case t =>
            Future.failed(t)
        }
  }
}
