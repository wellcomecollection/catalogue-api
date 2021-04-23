package uk.ac.wellcome.display

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
  val indexDate = "2021-04-19"

  def apply(): ElasticConfig =
    ElasticConfig(
      worksIndex = Index(s"works-$indexDate"),
      imagesIndex = Index(s"images-$indexDate")
    )
}
