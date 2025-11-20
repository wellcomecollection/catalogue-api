package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.{ElasticClient, Handler, Response}
import grizzled.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}

class ResilientElasticClient(
  clientFactory: () => ElasticClient
)(implicit ec: ExecutionContext) extends Logging {

  @volatile private var client: ElasticClient = clientFactory()

  def execute[T, U](t: T)(implicit handler: Handler[T, U], manifest: Manifest[U]): Future[Response[U]] = {
    client.execute(t).flatMap { response =>
      response.status match {
        case 401 | 403 =>
          warn(s"Received ${response.status} from Elasticsearch, refreshing client and retrying...")
          refreshClient()
          client.execute(t)
        case _ => Future.successful(response)
      }
    }
  }

  def close(): Unit = {
    client.close()
  }

  private def refreshClient(): Unit = {
    synchronized {
      val oldClient = client
      try {
        info("Refreshing Elasticsearch client...")
        client = clientFactory()
        oldClient.close()
        info("Elasticsearch client refreshed.")
      } catch {
        case e: Throwable =>
          error("Failed to refresh Elasticsearch client", e)
          throw e
      }
    }
  }
}
