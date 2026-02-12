package weco.api.search.models

import com.sksamuel.elastic4s.Index

case class ElasticConfig(
  name: String = "default",
  hostSecretPath: String,
  apiKeySecretPath: String,
  portSecretPath: String,
  protocolSecretPath: String,
  worksIndex: Option[Index] = None,
  imagesIndex: Option[Index] = None,
  semanticConfig: Option[SemanticConfig] = None
)

object ElasticConfig {
  // Default values shared across the API
  // We use this to share config across Scala API applications
  // i.e. The API and the snapshot generator.
  val defaultPipelineDate = "2025-10-02"
  val defaultWorksIndexDate = "2025-11-20"
  val defaultImagesIndexDate = "2025-10-02"

  def forDefaultCluster(
    pipelineDate: String = defaultPipelineDate,
    worksIndexName: Option[String] = None,
    imagesIndexName: Option[String] = None,
    serviceName: String,
    environment: ApiEnvironment = ApiEnvironment.Prod,
    semanticConfig: Option[SemanticConfig] = None
  ): ElasticConfig = {
    val pipelinePrefix = s"elasticsearch/pipeline_storage_$pipelineDate";
    val hostType = environment match {
      case ApiEnvironment.Dev => "public_host"
      case _                  => "private_host"
    }

    val worksIndex = Index(worksIndexName.getOrElse(s"works-indexed-$defaultWorksIndexDate"))
    val imagesIndex = Index(imagesIndexName.getOrElse(s"images-indexed-$defaultImagesIndexDate"))

    new ElasticConfig(
      worksIndex = Some(worksIndex),
      imagesIndex = Some(imagesIndex),
      hostSecretPath = s"$pipelinePrefix/$hostType",
      apiKeySecretPath = s"$pipelinePrefix/$serviceName/api_key",
      portSecretPath = s"$pipelinePrefix/port",
      protocolSecretPath = s"$pipelinePrefix/protocol",
      semanticConfig = semanticConfig
    )
  }
}

sealed trait VectorType
object VectorType {
  case object Dense extends VectorType
  case object Sparse extends VectorType
}

case class SemanticConfig(
  modelId: String,
  vectorType: VectorType,
  k: Int = 50,
  numCandidates: Int = 500,
  rankWindowSize: Int = 10000,
  rankConstant: Int = 20,
)
