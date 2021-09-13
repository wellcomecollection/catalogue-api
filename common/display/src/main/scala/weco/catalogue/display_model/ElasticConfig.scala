package weco.catalogue.display_model

import com.sksamuel.elastic4s.Index

// This is here as display is the only module shared
// between api and snapshot_generator.
case class ElasticConfig(
  worksIndex: Index,
  imagesIndex: Index
)

object ElasticConfig {
  // We use this to share config across API applications
  // i.e. The API and the snapshot generator.
  //
  // We sometimes append a suffix to the index names (e.g. 2001-01-01a)
  // if we've had to create a new index attached to an existing pipeline.
  // This can occur if cross-cluster replication breaks between the
  // pipeline and API clusters.
  val pipelineDate = "2021-08-16"
  val suffix = "a"

  def apply(): ElasticConfig =
    ElasticConfig(
      worksIndex = Index(s"works-indexed-$pipelineDate$suffix"),
      imagesIndex = Index(s"images-indexed-$pipelineDate$suffix")
    )
}
