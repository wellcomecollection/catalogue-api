package uk.ac.wellcome.platform.snapshot_generator.akkastreams.source

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.sksamuel.elastic4s.ElasticClient
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.snapshot_generator.models.SnapshotGeneratorConfig
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed
import weco.catalogue.snapshot_generator.iterators.ElasticsearchWorksIterator

import scala.concurrent.duration._

object ElasticsearchWorksSource extends Logging {
  def apply(elasticClient: ElasticClient,
            snapshotConfig: SnapshotGeneratorConfig): Source[Work[Indexed], NotUsed] = {
    val iterator = new ElasticsearchWorksIterator()(
      client = elasticClient,
      timeout = 5 minutes
    )

    Source
      .fromIterator(() => iterator.scroll(snapshotConfig.index))
  }
}
