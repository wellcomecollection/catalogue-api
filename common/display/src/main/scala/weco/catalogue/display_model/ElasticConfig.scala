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
  val indexDate = "2021-08-09"

  def apply(): ElasticConfig =
    ElasticConfig(
      worksIndex = Index(s"works-indexed-$indexDate"),
      imagesIndex = Index(s"images-indexed-$indexDate")
    )
}
