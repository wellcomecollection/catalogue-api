package weco.catalogue.display_model

import com.sksamuel.elastic4s.Index

// This is here as display is the only module shared
// between api and snapshot_generator.
case class ElasticConfig(
  worksIndex: Index,
  imagesIndex: Index
)

trait ElasticConfigBase {
  // We use this to share config across API applications
  // i.e. The API and the snapshot generator.
  //
  // We have two Elasticsearch clusters we use:
  //
  //    - the pipeline cluster.  We create a new cluster per-pipeline, and this is
  //      where new documents are written by the ingestor.
  //    - the API cluster.  We have a single such cluster, and it gets updates from
  //      the pipeline cluster using cross-cluster-replication (CCR).
  //
  // Generally we use the same index name in the pipeline cluster and the API cluster,
  // e.g.
  //
  //      pipeline: works-indexed-2021-08-16
  //      api:      works-indexed-2021-08-16
  //
  // But sometimes we need to create new indexes within an existing pipeline,
  // e.g. if cross-cluster replication breaks.  Then we append a suffix to the name
  // in the API cluster, but keep the existing name in the pipeline cluster, e.g.
  //
  //      pipeline: works-indexed-2021-08-16
  //      api:      works-indexed-2021-08-16a
  //
  // The different config allows applications to choose whether they want to read
  // from the pipeline cluster or the API cluster.
  val pipelineDate = "2022-03-10"
  val suffix = ""
}

object PipelineClusterElasticConfig extends ElasticConfigBase {
  def apply(): ElasticConfig =
    ElasticConfig(
      worksIndex = Index(s"works-indexed-$pipelineDate"),
      imagesIndex = Index(s"images-indexed-$pipelineDate")
    )
}

object ApiClusterElasticConfig extends ElasticConfigBase {
  def apply(): ElasticConfig =
    ElasticConfig(
      worksIndex = Index(s"works-indexed-$pipelineDate$suffix"),
      imagesIndex = Index(s"images-indexed-$pipelineDate$suffix")
    )
}
