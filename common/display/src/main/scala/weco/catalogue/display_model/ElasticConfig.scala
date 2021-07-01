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
  val indexDate = "2021-06-27"

  // This index has been manually created as a workaround for performance issue
  // TODO: This line should be removed when the index moves on from 2021-06-27
  val temporaryWorksIndex = s"works-indexed-2021-06-27a"

  def apply(): ElasticConfig =
    ElasticConfig(
      // worksIndex = Index(s"works-indexed-$indexDate"),
      // TODO: This line should be removed and the one above uncommented when the index moves on from 2021-06-27
      worksIndex = Index(temporaryWorksIndex),
      imagesIndex = Index(s"images-indexed-$indexDate")
    )
}
