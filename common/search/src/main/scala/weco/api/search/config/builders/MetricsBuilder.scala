package weco.api.search.config.builders

import weco.monitoring.Metrics
import weco.monitoring.memory.MemoryMetrics
import weco.monitoring.typesafe.CloudWatchBuilder

import akka.stream.Materializer
import com.typesafe.config.Config

import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext

/**
  * Build an appropriate Metrics object for use by weco.http.monitoring.HttpMetrics
  * For a "Real" instance, publish metrics on CloudWatch,
  * For a local or experimental instance, simply store them in memory.
  */
object MetricsBuilder {
  def apply(config: Config)(
    implicit
    materializer: Materializer,
    ec: ExecutionContext
  ): Metrics[Future] =
    sys.env.get("API_METRICS_MODE") match {
      case None | Some("cloudwatch") => CloudWatchBuilder.buildCloudWatchMetrics(config)
      case Some("memory") => new MemoryMetrics
    }
}
