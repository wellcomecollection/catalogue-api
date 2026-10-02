package weco.api.search.models

import com.sksamuel.elastic4s.Index

case class ElasticConfig(
  name: String = "default",
  pipelineDate: Option[String] = None,
  worksIndex: Option[String] = None,
  imagesIndex: Option[String] = None,
  hostSecretPath: Option[String] = None,
  apiKeySecretPath: Option[String] = None,
  portSecretPath: Option[String] = None,
  protocolSecretPath: Option[String] = None
) {

  def getPipelineDate: String =
    pipelineDate.getOrElse(ElasticConfig.defaultPipelineDate)

  def getWorksIndex: Index =
    Index(
      worksIndex.getOrElse(
        s"works-indexed-${ElasticConfig.defaultWorksIndexDate}"
      )
    )

  def getImagesIndex: Index =
    Index(
      imagesIndex.getOrElse(
        s"images-indexed-${ElasticConfig.defaultImagesIndexDate}"
      )
    )
}

object ElasticConfig {
  // Default values shared across the API
  // We use this to share config across Scala API applications
  // i.e. The API and the snapshot generator.
  val defaultPipelineDate = "2026-07-03"
  val defaultWorksIndexDate = "2026-07-03"
  val defaultImagesIndexDate = "2026-07-03"
}
