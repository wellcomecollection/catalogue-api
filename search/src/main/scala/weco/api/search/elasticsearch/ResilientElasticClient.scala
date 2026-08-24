package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.{ElasticClient, ElasticRequest, Handler, Response}
import grizzled.slf4j.Logging

import java.time.Clock
import java.util.concurrent.{Executors, ThreadFactory, TimeUnit}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class ResilientElasticClient(
  clientFactory: () => ElasticClient,
  minRefreshIntervalMs: Long = 2000,
  closeGraceMs: Long = 30000,
  probeTimeoutMs: Long = 5000
)(implicit clock: Clock, ec: ExecutionContext)
    extends Logging
    with AutoCloseable {

  @volatile private var client: ElasticClient = clientFactory()
  @volatile private var lastRefreshTime: Long = 0

  // Dedicated thread keeps the factory's blocking Secrets Manager IO off the request dispatcher
  private val refreshExecutor = Executors.newSingleThreadScheduledExecutor(
    new ThreadFactory {
      override def newThread(r: Runnable): Thread = {
        val thread = new Thread(r, "resilient-elastic-client-refresh")
        thread.setDaemon(true)
        thread
      }
    }
  )
  private val refreshEc = ExecutionContext.fromExecutor(refreshExecutor)

  private val refreshLock = new Object
  private var inFlightRefresh: Option[Future[Unit]] = None
  private var pendingClose: Option[ElasticClient] = None

  def execute[T, U](t: T)(implicit handler: Handler[T, U],
                          manifest: Manifest[U]): Future[Response[U]] = {
    val currentClient = client
    currentClient.execute(t).transformWith {
      case Success(response)
          if response.status == 401 || response.status == 403 =>
        warn(
          s"Received ${response.status} from Elasticsearch, refreshing client and retrying...")
        retryIfRefreshed(currentClient, t, response)
      case Success(response)                               => Future.successful(response)
      case Failure(NonFatal(e)) if client ne currentClient =>
        // Our client was replaced mid-flight and likely closed under us; retry
        // through execute so a 401 on the retry still gets refresh handling.
        // Re-entry is bounded: each level requires another swap to have happened.
        warn(
          "Request failed on a replaced Elasticsearch client, retrying on the current one",
          e)
        execute(t)
      // Transport errors are not rotation evidence: fail fast, no refresh
      case Failure(e) => Future.failed(e)
    }
  }

  // Join the coalesced refresh, then retry only if the client actually changed:
  // a retry on the same client is futile and just duplicates load
  private def retryIfRefreshed[T, U](failedClient: ElasticClient,
                                     t: T,
                                     originalResponse: Response[U])(
    implicit handler: Handler[T, U],
    manifest: Manifest[U]): Future[Response[U]] =
    triggerRefresh(failedClient).flatMap { _ =>
      val current = client
      if (current ne failedClient) current.execute(t)
      else Future.successful(originalResponse)
    }

  // Coalesces concurrent requests onto one in-flight refresh instead of queueing on a lock
  private def triggerRefresh(failedClient: ElasticClient): Future[Unit] =
    refreshLock.synchronized {
      inFlightRefresh.getOrElse {
        if (client ne failedClient) {
          info("Elasticsearch client already refreshed by another thread.")
          Future.unit
        } else {
          val now = clock.millis()
          if (now - lastRefreshTime > minRefreshIntervalMs) {
            val refresh = doRefresh()
            inFlightRefresh = Some(refresh)
            refresh.onComplete { _ =>
              refreshLock.synchronized { inFlightRefresh = None }
            }(refreshEc)
            refresh
          } else {
            warn(
              s"Refresh requested too soon (last refresh ${now - lastRefreshTime}ms ago). " +
                s"Skipping, waiting on cooldown: ${minRefreshIntervalMs}ms"
            )
            Future.unit
          }
        }
      }
    }

  private def doRefresh(): Future[Unit] = {
    info("Refreshing Elasticsearch client...")
    Future(clientFactory())(refreshEc)
      .flatMap { candidate =>
        probeAccepts(candidate).map { accepted =>
          if (accepted) swapIn(candidate)
          else {
            // Secrets Manager may still be serving the invalidated key; keep the
            // current client and probe again on the next post-cooldown failure
            warn("Rebuilt Elasticsearch client failed to authenticate; keeping current client.")
            if (candidate ne client) closeQuietly(candidate)
          }
        }(refreshEc)
      }(refreshEc)
      .recover {
        case NonFatal(e) => error("Failed to refresh Elasticsearch client", e)
      }(refreshEc)
      // Throttle the next refresh attempt whether or not this one healed us
      .andThen { case _ => lastRefreshTime = clock.millis() }(refreshEc)
  }

  // Reject only a definitive 401/403: an inconclusive probe (5xx, 429, 404, timeout,
  // exception) must not block adopting a possibly-good key during a cluster incident,
  // and security-disabled clusters answer 4xx/404 here
  private def probeAccepts(candidate: ElasticClient): Future[Boolean] = {
    val promise = Promise[Boolean]()
    candidate.client.send(
      ElasticRequest("GET", "/_security/_authenticate"), {
        case Right(response) =>
          promise.trySuccess(
            response.statusCode != 401 && response.statusCode != 403)
        case Left(_) => promise.trySuccess(true)
      }
    )
    val timeout = refreshExecutor.schedule(new Runnable {
      override def run(): Unit = promise.trySuccess(true)
    }, probeTimeoutMs, TimeUnit.MILLISECONDS)
    // Don't leave a timer queued for every probe that answered promptly
    promise.future.andThen { case _ => timeout.cancel(false) }(refreshEc)
  }

  private def swapIn(candidate: ElasticClient): Unit = {
    val oldClient = client
    client = candidate
    // The fixture factory can return the same client, so never close it in that case
    if (oldClient ne candidate) deferClose(oldClient)
    info("Elasticsearch client refreshed.")
  }

  // Grace period lets in-flight requests on the old client complete; at most one
  // close is deferred, bounding connection-pool buildup if swaps repeat
  private def deferClose(oldClient: ElasticClient): Unit =
    refreshLock.synchronized {
      pendingClose.foreach(closeQuietly)
      pendingClose = Some(oldClient)
      refreshExecutor.schedule(
        new Runnable {
          override def run(): Unit =
            refreshLock.synchronized {
              if (pendingClose.exists(_ eq oldClient)) {
                pendingClose = None
                closeQuietly(oldClient)
              }
            }
        },
        closeGraceMs,
        TimeUnit.MILLISECONDS
      )
    }

  private def closeQuietly(c: ElasticClient): Unit =
    try c.close()
    catch {
      case NonFatal(e) => warn("Failed to close Elasticsearch client", e)
    }

  override def close(): Unit = {
    refreshExecutor.shutdownNow()
    refreshLock.synchronized {
      pendingClose.foreach(closeQuietly)
      pendingClose = None
    }
    closeQuietly(client)
  }
}
