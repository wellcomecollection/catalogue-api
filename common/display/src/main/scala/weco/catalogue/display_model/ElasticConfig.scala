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
  // TODO: These lines should be removed when the index moves on from 2021-06-27
  val temporaryWorksIndex = s"works-indexed-2021-06-27b"
  val temporaryImagesIndex = s"images-indexed-2021-06-27b"

  def apply(): ElasticConfig =
    ElasticConfig(
      // worksIndex = Index(s"works-indexed-$indexDate"),
      // imagesIndex = Index(s"images-indexed-$indexDate")
      // TODO: these lines should be removed and the above uncommented when the index moves on from 2021-06-27
      worksIndex = Index(temporaryWorksIndex),
      imagesIndex = Index(temporaryImagesIndex)
    )
}
