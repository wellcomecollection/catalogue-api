package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.{ElasticClient, Handler, Response}
import grizzled.slf4j.Logging

import java.time.Clock
import scala.concurrent.{ExecutionContext, Future}

class ResilientElasticClient(
  clientFactory: () => ElasticClient,
  minRefreshIntervalMs: Long = 2000
)(implicit clock: Clock, ec: ExecutionContext)
    extends Logging {

  @volatile private var client: ElasticClient = clientFactory()
  @volatile private var lastRefreshTime: Long = 0

  def execute[T, U](t: T)(implicit handler: Handler[T, U],
                          manifest: Manifest[U]): Future[Response[U]] = {
    val currentClient = client
    currentClient.execute(t).flatMap { response =>
      response.status match {
        case 401 | 403 =>
          warn(
            s"Received ${response.status} from Elasticsearch, refreshing client and retrying...")
          refreshClient(currentClient)
          client.execute(t)
        case _ => Future.successful(response)
      }
    }
  }

  private def refreshClient(failedClient: ElasticClient): Unit =
    synchronized {
      if (client == failedClient) {
        val now = clock.millis()
        if (now - lastRefreshTime > minRefreshIntervalMs) {
          val oldClient = client
          try {
            info("Refreshing Elasticsearch client...")
            client = clientFactory()
            lastRefreshTime = now
            oldClient.close()
            info("Elasticsearch client refreshed.")
          } catch {
            case e: Throwable =>
              error("Failed to refresh Elasticsearch client", e)
              throw e
          }
        } else {
          warn(
            s"Refresh requested too soon (last refresh ${now - lastRefreshTime}ms ago). " +
              s"Skipping, waiting on cooldown: ${minRefreshIntervalMs}ms"
          )
        }
      } else {
        info("Elasticsearch client already refreshed by another thread.")
      }
    }
}
